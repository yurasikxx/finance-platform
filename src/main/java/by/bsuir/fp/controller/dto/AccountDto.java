package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.AccountType;
import by.bsuir.fp.model.enums.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountDto {
    private Long id;

    @NotBlank(message = "Название счета обязательно")
    private String name;

    @NotNull(message = "Тип счета обязателен")
    private AccountType type;

    @NotNull(message = "Валюта обязательна")
    private CurrencyCode currency;

    @NotNull(message = "Начальный баланс обязателен")
    @PositiveOrZero(message = "Баланс не может быть отрицательным")
    private BigDecimal initialBalance;

    private BigDecimal currentBalance;
    private String description;
}