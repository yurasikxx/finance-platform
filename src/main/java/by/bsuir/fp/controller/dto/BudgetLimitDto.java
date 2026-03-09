package by.bsuir.fp.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BudgetLimitDto {
    private Long id;

    @NotNull(message = "ID категории обязателен")
    private Long categoryId;

    private String categoryName;
    private String categoryColor;

    @NotNull(message = "Лимит обязателен")
    @Positive(message = "Лимит должен быть положительным")
    private BigDecimal limitAmount;

    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private Integer progressPercentage;
    private String progressStatus; // "normal", "warning", "danger"
}