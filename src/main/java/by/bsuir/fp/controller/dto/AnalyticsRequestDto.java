package by.bsuir.fp.controller.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class AnalyticsRequestDto {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate fromDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate toDate;

    private Long categoryId;
    private Long accountId;
    private String period = "month"; // month, quarter, year
}