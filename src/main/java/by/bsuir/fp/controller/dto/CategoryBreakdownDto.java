package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CategoryBreakdownDto {
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private BigDecimal amount;
    private BigDecimal percentage;
}