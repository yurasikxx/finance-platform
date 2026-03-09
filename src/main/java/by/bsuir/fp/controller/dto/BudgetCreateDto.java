package by.bsuir.fp.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class BudgetCreateDto {
    @NotBlank(message = "Название бюджета обязательно")
    private String name;

    @NotNull(message = "Месяц обязателен")
    private Integer periodMonth;

    @NotNull(message = "Год обязателен")
    private Integer periodYear;

    private BigDecimal plannedIncome;
    private String description;

    // Map, где ключ - ID категории, значение - лимит
    private Map<Long, BigDecimal> categoryLimits;
}