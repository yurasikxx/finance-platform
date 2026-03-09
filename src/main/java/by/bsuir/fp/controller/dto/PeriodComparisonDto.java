package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PeriodComparisonDto {
    private String periodName;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private BigDecimal incomeChange; // в процентах
    private BigDecimal expenseChange; // в процентах
}