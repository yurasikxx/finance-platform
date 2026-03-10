package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.CurrencyCode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private CurrencyCode defaultCurrency;
    private LocalDateTime createdAt;
    private String googleId;
}