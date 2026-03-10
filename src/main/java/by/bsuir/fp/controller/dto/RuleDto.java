package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.RuleOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDto {
    private Long id;

    @NotNull(message = "ID категории обязателен")
    private Long categoryId;

    private String categoryName;

    @NotBlank(message = "Поле для проверки обязательно")
    private String field;  // "description", "amount"

    @NotNull(message = "Оператор обязателен")
    private RuleOperator operator;

    @NotBlank(message = "Значение обязательно")
    private String value;

    private Integer priority;
    private Boolean isActive;
}