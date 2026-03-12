package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.*;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.TransactionRepository;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;

    @Transactional(readOnly = true)
    public AnalyticsDashboardDto getDashboard(Long userId, AnalyticsRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        LocalDate fromDate = request.getFromDate();
        LocalDate toDate = request.getToDate();

        if (fromDate == null || toDate == null) {
            LocalDate now = LocalDate.now();
            fromDate = now.withDayOfMonth(1);
            toDate = now;
        }

        CurrencyCode baseCurrency = user.getDefaultCurrency();

        List<Transaction> transactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, fromDate, toDate);

        BigDecimal totalIncome = calculateTotalInBaseCurrency(transactions, TransactionType.INCOME, baseCurrency);
        BigDecimal totalExpense = calculateTotalInBaseCurrency(transactions, TransactionType.EXPENSE, baseCurrency);

        List<CategoryBreakdownDto> expenseBreakdown = getCategoryBreakdownInBaseCurrency(
                transactions, TransactionType.EXPENSE, baseCurrency);
        List<CategoryBreakdownDto> incomeBreakdown = getCategoryBreakdownInBaseCurrency(
                transactions, TransactionType.INCOME, baseCurrency);

        List<DailyTotalDto> dailyTotals = getDailyTotalsInBaseCurrency(transactions, fromDate, toDate, baseCurrency);

        PeriodComparisonDto previousPeriod = getPreviousPeriodComparison(user, fromDate, toDate, baseCurrency);
        PeriodComparisonDto samePeriodLastYear = getSamePeriodLastYearComparison(user, fromDate, toDate, baseCurrency);

        return AnalyticsDashboardDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netBalance(totalIncome.subtract(totalExpense))
                .expenseBreakdown(expenseBreakdown)
                .incomeBreakdown(incomeBreakdown)
                .dailyTotals(dailyTotals)
                .previousPeriod(previousPeriod)
                .samePeriodLastYear(samePeriodLastYear)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownDto> getCategoryBreakdownInBaseCurrency(
            Long userId, TransactionType type, LocalDate fromDate, LocalDate toDate, CurrencyCode baseCurrency) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Transaction> transactions = transactionRepository
                .findByUserAndTypeAndTransactionDateBetween(user, type, fromDate, toDate);

        return getCategoryBreakdownInBaseCurrency(transactions, type, baseCurrency);
    }

    private List<CategoryBreakdownDto> getCategoryBreakdownInBaseCurrency(
            List<Transaction> transactions, TransactionType type, CurrencyCode baseCurrency) {

        Map<Category, BigDecimal> categorySums = new HashMap<>();

        for (Transaction tx : transactions) {
            if (tx.getType() == type && tx.getCategory() != null) {
                BigDecimal amountInBase = currencyService.convertTransaction(tx, baseCurrency);
                categorySums.merge(tx.getCategory(), amountInBase, BigDecimal::add);
            }
        }

        BigDecimal total = categorySums.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownDto> breakdown = new ArrayList<>();

        for (Map.Entry<Category, BigDecimal> entry : categorySums.entrySet()) {
            Category category = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0 ?
                    amount.multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            breakdown.add(CategoryBreakdownDto.builder()
                    .categoryId(category.getId())
                    .categoryName(category.getName())
                    .categoryColor(category.getColor())
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        return breakdown.stream()
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotalInBaseCurrency(
            List<Transaction> transactions, TransactionType type, CurrencyCode baseCurrency) {

        return transactions.stream()
                .filter(tx -> tx.getType() == type)
                .map(tx -> currencyService.convertTransaction(tx, baseCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DailyTotalDto> getDailyTotalsInBaseCurrency(
            List<Transaction> transactions, LocalDate fromDate, LocalDate toDate, CurrencyCode baseCurrency) {

        Map<LocalDate, DailyTotalDto> dailyMap = new LinkedHashMap<>();

        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            dailyMap.put(current, DailyTotalDto.builder()
                    .date(current)
                    .income(BigDecimal.ZERO)
                    .expense(BigDecimal.ZERO)
                    .balance(BigDecimal.ZERO)
                    .build());
            current = current.plusDays(1);
        }

        for (Transaction tx : transactions) {
            DailyTotalDto dto = dailyMap.get(tx.getTransactionDate());
            if (dto != null) {
                BigDecimal amountInBase = currencyService.convertTransaction(tx, baseCurrency);

                if (tx.getType() == TransactionType.INCOME) {
                    dto.setIncome(dto.getIncome().add(amountInBase));
                } else if (tx.getType() == TransactionType.EXPENSE) {
                    dto.setExpense(dto.getExpense().add(amountInBase));
                }
                dto.setBalance(dto.getIncome().subtract(dto.getExpense()));
            }
        }

        return new ArrayList<>(dailyMap.values());
    }

    private PeriodComparisonDto getPreviousPeriodComparison(
            User user, LocalDate fromDate, LocalDate toDate, CurrencyCode baseCurrency) {

        long daysDiff = ChronoUnit.DAYS.between(fromDate, toDate) + 1;

        LocalDate prevTo = fromDate.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(daysDiff - 1);

        List<Transaction> currentTransactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, fromDate, toDate);

        List<Transaction> prevTransactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, prevFrom, prevTo);

        BigDecimal currentIncome = calculateTotalInBaseCurrency(currentTransactions, TransactionType.INCOME, baseCurrency);
        BigDecimal currentExpense = calculateTotalInBaseCurrency(currentTransactions, TransactionType.EXPENSE, baseCurrency);
        BigDecimal currentBalance = currentIncome.subtract(currentExpense);

        BigDecimal prevIncome = calculateTotalInBaseCurrency(prevTransactions, TransactionType.INCOME, baseCurrency);
        BigDecimal prevExpense = calculateTotalInBaseCurrency(prevTransactions, TransactionType.EXPENSE, baseCurrency);
        BigDecimal prevBalance = prevIncome.subtract(prevExpense);

        BigDecimal incomeChange = calculatePercentageChange(currentIncome, prevIncome);
        BigDecimal expenseChange = calculatePercentageChange(currentExpense, prevExpense);

        String periodName = prevFrom.format(DateTimeFormatter.ofPattern("dd.MM")) + " - " +
                prevTo.format(DateTimeFormatter.ofPattern("dd.MM"));

        return PeriodComparisonDto.builder()
                .periodName(periodName)
                .currentIncome(currentIncome)
                .currentExpense(currentExpense)
                .currentBalance(currentBalance)
                .previousIncome(prevIncome)
                .previousExpense(prevExpense)
                .previousBalance(prevBalance)
                .incomeChange(incomeChange)
                .expenseChange(expenseChange)
                .build();
    }

    private PeriodComparisonDto getSamePeriodLastYearComparison(
            User user, LocalDate fromDate, LocalDate toDate, CurrencyCode baseCurrency) {

        LocalDate lastYearFrom = fromDate.minusYears(1);
        LocalDate lastYearTo = toDate.minusYears(1);

        List<Transaction> currentTransactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, fromDate, toDate);

        List<Transaction> lastYearTransactions = transactionRepository
                .findByUserAndTransactionDateBetween(user, lastYearFrom, lastYearTo);

        BigDecimal currentIncome = calculateTotalInBaseCurrency(currentTransactions, TransactionType.INCOME, baseCurrency);
        BigDecimal currentExpense = calculateTotalInBaseCurrency(currentTransactions, TransactionType.EXPENSE, baseCurrency);
        BigDecimal currentBalance = currentIncome.subtract(currentExpense);

        BigDecimal lastYearIncome = calculateTotalInBaseCurrency(lastYearTransactions, TransactionType.INCOME, baseCurrency);
        BigDecimal lastYearExpense = calculateTotalInBaseCurrency(lastYearTransactions, TransactionType.EXPENSE, baseCurrency);
        BigDecimal lastYearBalance = lastYearIncome.subtract(lastYearExpense);

        BigDecimal incomeChange = calculatePercentageChange(currentIncome, lastYearIncome);
        BigDecimal expenseChange = calculatePercentageChange(currentExpense, lastYearExpense);

        return PeriodComparisonDto.builder()
                .periodName("Аналогичный период прошлого года")
                .currentIncome(currentIncome)
                .currentExpense(currentExpense)
                .currentBalance(currentBalance)
                .previousIncome(lastYearIncome)
                .previousExpense(lastYearExpense)
                .previousBalance(lastYearBalance)
                .incomeChange(incomeChange)
                .expenseChange(expenseChange)
                .build();
    }

    private BigDecimal calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ?
                    BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}