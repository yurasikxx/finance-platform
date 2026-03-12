package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.AccountDto;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountRestController {

    private final SecurityService securityService;
    private final UserService userService;
    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<AccountDto>> getUserAccounts() {
        Long userId = securityService.getCurrentUserId();
        List<AccountDto> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        Long userId = securityService.getCurrentUserId();
        AccountDto account = accountService.getAccountById(userId, id);
        return ResponseEntity.ok(account);
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@Valid @RequestBody AccountDto accountDto) {
        Long userId = securityService.getCurrentUserId();
        AccountDto created = accountService.createAccount(userId, accountDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDto> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto accountDto) {
        Long userId = securityService.getCurrentUserId();
        AccountDto updated = accountService.updateAccount(userId, id, accountDto);
        return ResponseEntity.ok(updated);
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
        var user = userService.getUserById(userId);
        BigDecimal totalBalance = accountService.getTotalBalanceInBaseCurrency(userId, user.getDefaultCurrency());
        return ResponseEntity.ok(totalBalance);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countUserAccounts() {
        Long userId = securityService.getCurrentUserId();
        long count = accountService.countUserAccounts(userId);
        return ResponseEntity.ok(count);
    }
}