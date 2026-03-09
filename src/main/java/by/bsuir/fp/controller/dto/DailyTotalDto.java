package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class DailyTotalDto {
    private LocalDate date;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
}