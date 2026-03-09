package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.CurrencyCode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CurrencyRateDto {
    private CurrencyCode currency;
    private BigDecimal rateToByn;
    private LocalDate rateDate;
}