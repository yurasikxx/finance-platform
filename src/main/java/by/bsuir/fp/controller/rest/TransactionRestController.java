package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.ImportResult;
import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.service.ImportService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final TransactionService transactionService;
    private final ImportService importService;
    private final SecurityService securityService;

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(TransactionFilterDto filter) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.getTransactions(userId, filter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.getTransactionById(userId, id));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@Valid @RequestBody TransactionDto transactionDto) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.createTransaction(userId, transactionDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDto transactionDto) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.updateTransaction(userId, id, transactionDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/uncategorized")
    public ResponseEntity<List<TransactionDto>> getUncategorizedTransactions() {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.getUncategorizedTransactions(userId));
    }

    @PostMapping("/{id}/categorize")
    public ResponseEntity<TransactionDto> categorizeTransaction(
            @PathVariable Long id,
            @RequestParam Long categoryId) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.categorizeTransaction(userId, id, categoryId));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<Map<String, BigDecimal>> getDailyStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(transactionService.getDailyStats(userId, fromDate, toDate));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accountId") Long accountId) {

        Long userId = securityService.getCurrentUserId();
        ImportResult result = importService.importFromCsv(userId, accountId, file);

        return ResponseEntity.ok(result);
    }
}