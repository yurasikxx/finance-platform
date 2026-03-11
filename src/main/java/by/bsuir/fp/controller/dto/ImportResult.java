package by.bsuir.fp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ImportResult {
    private List<TransactionDto> imported;
    private List<String> errors;
    private int totalCount;
    private int successCount;
    private int errorCount;
}