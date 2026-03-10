package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.TransactionType;
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
public class CategoryDto {
    private Long id;

    @NotBlank(message = "Название категории обязательно")
    private String name;

    @NotNull(message = "Тип категории обязателен")
    private TransactionType type;

    private String color;
    private String icon;
    private String description;
    private Boolean isDefault;
}