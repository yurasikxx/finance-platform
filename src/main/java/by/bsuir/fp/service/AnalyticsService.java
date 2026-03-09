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
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

        LocalDate previousFromDate = fromDate.minusMonths(1);
        LocalDate previousToDate = toDate.minusMonths(1);

        LocalDate lastYearFromDate = fromDate.minusYears(1);
        LocalDate lastYearToDate = toDate.minusYears(1);

        BigDecimal totalIncome = transactionRepository.getTotalIncomeByPeriod(user, fromDate, toDate);
        BigDecimal totalExpense = transactionRepository.getTotalExpenseByCategoryAndPeriod(user, null, fromDate, toDate);

        List<Object[]> dailyData = transactionRepository.getDailyExpenses(user, fromDate, toDate);
        List<DailyTotalDto> dailyTotals = aggregateDailyTotals(dailyData, fromDate, toDate);

        List<CategoryBreakdownDto> expenseBreakdown = getCategoryBreakdown(user, TransactionType.EXPENSE, fromDate, toDate);
        List<CategoryBreakdownDto> incomeBreakdown = getCategoryBreakdown(user, TransactionType.INCOME, fromDate, toDate);

        PeriodComparisonDto previousPeriod = getPeriodComparison(user, previousFromDate, previousToDate, fromDate, toDate, "Предыдущий месяц");
        PeriodComparisonDto samePeriodLastYear = getPeriodComparison(user, lastYearFromDate, lastYearToDate, fromDate, toDate, "Прошлый год");

        return AnalyticsDashboardDto.builder()
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .totalExpense(totalExpense != null ? totalExpense : BigDecimal.ZERO)
                .netBalance((totalIncome != null ? totalIncome : BigDecimal.ZERO)
                        .subtract(totalExpense != null ? totalExpense : BigDecimal.ZERO))
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

        List<Object[]> results = new ArrayList<>();

        if (type == TransactionType.EXPENSE) {
            results = transactionRepository.findByUserAndTypeAndTransactionDateBetweenGroupByCategory(
                    user, type, fromDate, toDate);
        }

        BigDecimal total = results.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return results.stream()
                .map(row -> {
                    Category category = (Category) row[0];
                    BigDecimal amount = (BigDecimal) row[1];
                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0 ?
                            amount.multiply(BigDecimal.valueOf(100))
                                    .divide(total, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    return CategoryBreakdownDto.builder()
                            .categoryId(category.getId())
                            .categoryName(category.getName())
                            .categoryColor(category.getColor())
                            .amount(amount)
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getAmount().compareTo(a.getAmount()))
                .collect(Collectors.toList());
    }

    private List<DailyTotalDto> aggregateDailyTotals(List<Object[]> dailyData, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, DailyTotalDto> dailyMap = new LinkedHashMap<>();

        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            DailyTotalDto dto = DailyTotalDto.builder()
                    .date(date)
                    .income(BigDecimal.ZERO)
                    .expense(BigDecimal.ZERO)
                    .balance(BigDecimal.ZERO)
                    .build();
            dailyMap.put(date, dto);
        }

        for (Object[] row : dailyData) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal amount = (BigDecimal) row[1];

            DailyTotalDto dto = dailyMap.get(date);
            if (dto != null) {
                dto.setExpense(amount);
                dto.setBalance(dto.getBalance().subtract(amount));
            }
        }

        return new ArrayList<>(dailyMap.values());
    }

    private PeriodComparisonDto getPeriodComparison(
            User user, LocalDate prevFrom, LocalDate prevTo,
            LocalDate currFrom, LocalDate currTo, String periodName) {

        BigDecimal prevIncome = transactionRepository.getTotalIncomeByPeriod(user, prevFrom, prevTo);
        BigDecimal prevExpense = transactionRepository.getTotalExpenseByCategoryAndPeriod(user, null, prevFrom, prevTo);

        BigDecimal currIncome = transactionRepository.getTotalIncomeByPeriod(user, currFrom, currTo);
        BigDecimal currExpense = transactionRepository.getTotalExpenseByCategoryAndPeriod(user, null, currFrom, currTo);

        prevIncome = prevIncome != null ? prevIncome : BigDecimal.ZERO;
        prevExpense = prevExpense != null ? prevExpense : BigDecimal.ZERO;
        currIncome = currIncome != null ? currIncome : BigDecimal.ZERO;
        currExpense = currExpense != null ? currExpense : BigDecimal.ZERO;

        BigDecimal incomeChange = calculatePercentageChange(currIncome, prevIncome);
        BigDecimal expenseChange = calculatePercentageChange(currExpense, prevExpense);

        return PeriodComparisonDto.builder()
                .periodName(periodName)
                .income(currIncome)
                .expense(currExpense)
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