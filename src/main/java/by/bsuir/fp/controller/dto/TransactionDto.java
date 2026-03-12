package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private Long id;

    @NotNull(message = "ID счета обязателен")
    private Long accountId;

    private String accountName;
    private CurrencyCode accountCurrency;

    private Long categoryId;
    private String categoryName;
    private String categoryColor;

    @NotNull(message = "Тип транзакции обязателен")
    private TransactionType type;

    @NotNull(message = "Сумма обязательна")
    @Positive(message = "Сумма должна быть положительной")
    private BigDecimal amount;

    private BigDecimal amountInBaseCurrency;

    @NotNull(message = "Дата обязательна")
    private LocalDate transactionDate;

    private String description;
    private Boolean isCategorized;
}