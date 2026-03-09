package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TransactionDto {
    private Long id;

    @NotNull(message = "ID счета обязателен")
    private Long accountId;

    private Long categoryId;
    private String categoryName;

    @NotNull(message = "Тип транзакции обязателен")
    private TransactionType type;

    @NotNull(message = "Сумма обязательна")
    @Positive(message = "Сумма должна быть положительной")
    private BigDecimal amount;

    @NotNull(message = "Дата обязательна")
    private LocalDate transactionDate;

    private String description;
    private Boolean isCategorized;
}