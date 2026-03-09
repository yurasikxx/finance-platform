package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AnalyticsDashboardDto {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netBalance;
    private List<CategoryBreakdownDto> expenseBreakdown;
    private List<CategoryBreakdownDto> incomeBreakdown;
    private List<DailyTotalDto> dailyTotals;
    private PeriodComparisonDto previousPeriod;
    private PeriodComparisonDto samePeriodLastYear;
}