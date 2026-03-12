package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.AccountDto;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.service.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final AccountService accountService;
    private final SecurityService securityService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getUserAccounts() {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(accountService.getAccountById(userId, id));
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto accountDto) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(accountService.createAccount(userId, accountDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto accountDto) {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(accountService.updateAccount(userId, id, accountDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        accountService.deleteAccount(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balance/total")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        Long userId = securityService.getCurrentUserId();
        return ResponseEntity.ok(accountService.getTotalBalance(userId));
    }
}