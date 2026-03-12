package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.ImportResult;
import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.service.ImportService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.TransactionService;
import by.bsuir.fp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionRestController {

    private final SecurityService securityService;
    private final UserService userService;
    private final TransactionService transactionService;
    private final ImportService importService;

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getTransactions(TransactionFilterDto filter) {
        Long userId = securityService.getCurrentUserId();
        Page<TransactionDto> transactions = transactionService.getTransactions(userId, filter);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        TransactionDto transaction = transactionService.getTransactionById(userId, id);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(@Valid @RequestBody TransactionDto transactionDto) {
        Long userId = securityService.getCurrentUserId();
        TransactionDto created = transactionService.createTransaction(userId, transactionDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionDto transactionDto) {
        Long userId = securityService.getCurrentUserId();
        TransactionDto updated = transactionService.updateTransaction(userId, id, transactionDto);
        return ResponseEntity.ok(updated);
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
        List<TransactionDto> transactions = transactionService.getUncategorizedTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/{id}/categorize")
    public ResponseEntity<TransactionDto> categorizeTransaction(
            @PathVariable Long id,
            @RequestParam Long categoryId) {
        Long userId = securityService.getCurrentUserId();
        TransactionDto transaction = transactionService.categorizeTransaction(userId, id, categoryId);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/stats/monthly")
    public ResponseEntity<Map<String, BigDecimal>> getMonthlyStats() {
        Long userId = securityService.getCurrentUserId();
        var user = userService.getUserById(userId);

        Map<String, BigDecimal> stats = transactionService.getMonthlyStatsInBaseCurrency(
                userId, user.getDefaultCurrency()
        );

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<Map<String, Object>> getDailyStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Long userId = securityService.getCurrentUserId();
        var user = userService.getUserById(userId);

        Map<LocalDate, BigDecimal> dailyExpenses = transactionService.getDailyExpensesInBaseCurrency(
                userId, fromDate, toDate, user.getDefaultCurrency()
        );

        Map<LocalDate, BigDecimal> dailyIncomes = transactionService.getDailyIncomesInBaseCurrency(
                userId, fromDate, toDate, user.getDefaultCurrency()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("expenses", dailyExpenses);
        response.put("incomes", dailyIncomes);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countUserTransactions() {
        Long userId = securityService.getCurrentUserId();
        long count = transactionService.countUserTransactions(userId);
        return ResponseEntity.ok(count);
    }

    @PostMapping(value = "/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importTransactions(
            @RequestParam("file") MultipartFile file,
            @RequestParam("accountId") Long accountId) {

        Long userId = securityService.getCurrentUserId();
        ImportResult result = importService.importFromCsv(userId, accountId, file);

        return ResponseEntity.ok(result);
    }
}