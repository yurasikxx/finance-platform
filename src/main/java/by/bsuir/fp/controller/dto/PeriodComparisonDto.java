package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PeriodComparisonDto {
    private String periodName;
    private BigDecimal balance;
    private BigDecimal currentIncome;
    private BigDecimal currentExpense;
    private BigDecimal previousIncome;
    private BigDecimal previousExpense;
    private BigDecimal incomeChange;
    private BigDecimal expenseChange;
}