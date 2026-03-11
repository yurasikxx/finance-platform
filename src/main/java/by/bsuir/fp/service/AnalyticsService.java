package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.*;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

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

        BigDecimal totalIncome = transactionRepository.getTotalIncomeByPeriod(user, fromDate, toDate);
        BigDecimal totalExpense = transactionRepository.getTotalExpenseByPeriod(user, fromDate, toDate);

        totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;

        List<CategoryBreakdownDto> expenseBreakdown = getCategoryBreakdown(user, TransactionType.EXPENSE, fromDate, toDate);
        List<CategoryBreakdownDto> incomeBreakdown = getCategoryBreakdown(user, TransactionType.INCOME, fromDate, toDate);

        List<DailyTotalDto> dailyTotals = getDailyTotals(user, fromDate, toDate);

        PeriodComparisonDto previousPeriod = getPreviousPeriodComparison(user, fromDate, toDate);
        PeriodComparisonDto samePeriodLastYear = getSamePeriodLastYearComparison(user, fromDate, toDate);

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
    public List<CategoryBreakdownDto> getCategoryBreakdown(
            Long userId, TransactionType type, LocalDate fromDate, LocalDate toDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return getCategoryBreakdown(user, type, fromDate, toDate);
    }

    private List<CategoryBreakdownDto> getCategoryBreakdown(
            User user, TransactionType type, LocalDate fromDate, LocalDate toDate) {

        List<Object[]> results = transactionRepository
                .findByUserAndTypeAndTransactionDateBetweenGroupByCategory(user, type, fromDate, toDate);

        BigDecimal total = results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryBreakdownDto> breakdown = new ArrayList<>();

        for (Object[] row : results) {
            Category category = (Category) row[0];
            BigDecimal amount = (BigDecimal) row[1];
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

    private List<DailyTotalDto> getDailyTotals(User user, LocalDate fromDate, LocalDate toDate) {
        List<DailyTotalDto> dailyTotals = new ArrayList<>();

        List<Object[]> expenses = transactionRepository.getDailyExpenses(user, fromDate, toDate);
        List<Object[]> incomes = transactionRepository.getDailyIncomes(user, fromDate, toDate);

        Map<LocalDate, BigDecimal> expenseMap = new HashMap<>();
        Map<LocalDate, BigDecimal> incomeMap = new HashMap<>();

        for (Object[] row : expenses) {
            expenseMap.put((LocalDate) row[0], (BigDecimal) row[1]);
        }

        for (Object[] row : incomes) {
            incomeMap.put((LocalDate) row[0], (BigDecimal) row[1]);
        }

        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            BigDecimal expense = expenseMap.getOrDefault(current, BigDecimal.ZERO);
            BigDecimal income = incomeMap.getOrDefault(current, BigDecimal.ZERO);

            dailyTotals.add(DailyTotalDto.builder()
                    .date(current)
                    .income(income)
                    .expense(expense)
                    .balance(income.subtract(expense))
                    .build());

            current = current.plusDays(1);
        }

        return dailyTotals;
    }

    private PeriodComparisonDto getPreviousPeriodComparison(User user, LocalDate fromDate, LocalDate toDate) {
        LocalDate prevFrom = fromDate.minusMonths(1);
        LocalDate prevTo = toDate.minusMonths(1);

        BigDecimal prevIncome = transactionRepository.getTotalIncomeByPeriod(user, prevFrom, prevTo);
        BigDecimal prevExpense = transactionRepository.getTotalExpenseByPeriod(user, prevFrom, prevTo);

        BigDecimal currIncome = transactionRepository.getTotalIncomeByPeriod(user, fromDate, toDate);
        BigDecimal currExpense = transactionRepository.getTotalExpenseByPeriod(user, fromDate, toDate);

        prevIncome = prevIncome != null ? prevIncome : BigDecimal.ZERO;
        prevExpense = prevExpense != null ? prevExpense : BigDecimal.ZERO;
        currIncome = currIncome != null ? currIncome : BigDecimal.ZERO;
        currExpense = currExpense != null ? currExpense : BigDecimal.ZERO;

        BigDecimal incomeChange = calculatePercentageChange(currIncome, prevIncome);
        BigDecimal expenseChange = calculatePercentageChange(currExpense, prevExpense);

        String periodName = prevFrom.format(DateTimeFormatter.ofPattern("dd.MM")) + " - " +
                prevTo.format(DateTimeFormatter.ofPattern("dd.MM"));

        return PeriodComparisonDto.builder()
                .periodName(periodName)
                .currentIncome(currIncome)
                .currentExpense(currExpense)
                .previousIncome(prevIncome)
                .previousExpense(prevExpense)
                .incomeChange(incomeChange)
                .expenseChange(expenseChange)
                .build();
    }

    private PeriodComparisonDto getSamePeriodLastYearComparison(User user, LocalDate fromDate, LocalDate toDate) {
        LocalDate lastYearFrom = fromDate.minusYears(1);
        LocalDate lastYearTo = toDate.minusYears(1);

        BigDecimal lastYearIncome = transactionRepository.getTotalIncomeByPeriod(user, lastYearFrom, lastYearTo);
        BigDecimal lastYearExpense = transactionRepository.getTotalExpenseByPeriod(user, lastYearFrom, lastYearTo);

        BigDecimal currIncome = transactionRepository.getTotalIncomeByPeriod(user, fromDate, toDate);
        BigDecimal currExpense = transactionRepository.getTotalExpenseByPeriod(user, fromDate, toDate);

        lastYearIncome = lastYearIncome != null ? lastYearIncome : BigDecimal.ZERO;
        lastYearExpense = lastYearExpense != null ? lastYearExpense : BigDecimal.ZERO;
        currIncome = currIncome != null ? currIncome : BigDecimal.ZERO;
        currExpense = currExpense != null ? currExpense : BigDecimal.ZERO;

        BigDecimal incomeChange = calculatePercentageChange(currIncome, lastYearIncome);
        BigDecimal expenseChange = calculatePercentageChange(currExpense, lastYearExpense);

        return PeriodComparisonDto.builder()
                .periodName("Аналогичный период прошлого года")
                .currentIncome(currIncome)
                .currentExpense(currExpense)
                .balance(currIncome.subtract(currExpense))
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