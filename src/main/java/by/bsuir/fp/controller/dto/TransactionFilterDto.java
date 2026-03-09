package by.bsuir.fp.controller.dto;

import by.bsuir.fp.model.enums.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TransactionFilterDto {
    private Long accountId;
    private Long categoryId;
    private TransactionType type;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    private String searchText;
    private Integer page = 0;
    private Integer size = 20;
}