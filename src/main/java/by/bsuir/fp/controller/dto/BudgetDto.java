package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.BudgetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BudgetDto {
    private Long id;

    @NotBlank(message = "Название бюджета обязательно")
    private String name;

    @NotNull(message = "Месяц обязателен")
    private Integer periodMonth;

    @NotNull(message = "Год обязателен")
    private Integer periodYear;

    private BigDecimal plannedIncome;
    private BudgetStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;

    private List<BudgetLimitDto> limits;
    private BigDecimal totalSpent;
    private BigDecimal totalLimit;
    private BigDecimal remainingBudget;
    private Integer progressPercentage;
}