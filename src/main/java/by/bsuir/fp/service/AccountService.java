package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.AccountDto;
import by.bsuir.fp.exception.AccountAccessDeniedException;
import by.bsuir.fp.exception.AccountNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.AccountRepository;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final CurrencyService currencyService;

    @Transactional
    public AccountDto createAccount(Long userId, AccountDto accountDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Account account = Account.builder()
                .user(user)
                .name(accountDto.getName())
                .type(accountDto.getType())
                .currency(accountDto.getCurrency())
                .initialBalance(accountDto.getInitialBalance())
                .currentBalance(accountDto.getInitialBalance())
                .description(accountDto.getDescription())
                .build();

        Account savedAccount = accountRepository.save(account);
        return mapToDto(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccountAccessDeniedException("Нет доступа к этому счету");
        }

        return mapToDto(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return accountRepository.findByUser(user).stream()
                .map(this::mapToDto)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountDto updateAccount(Long userId, Long accountId, AccountDto accountDto) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccountAccessDeniedException("Нет доступа к этому счету");
        }

        account.setName(accountDto.getName());
        account.setType(accountDto.getType());
        account.setDescription(accountDto.getDescription());

        if (accountDto.getInitialBalance() != null) {
            BigDecimal balanceDiff = accountDto.getInitialBalance().subtract(account.getInitialBalance());
            account.setInitialBalance(accountDto.getInitialBalance());
            account.setCurrentBalance(account.getCurrentBalance().add(balanceDiff));
        }

        Account updatedAccount = accountRepository.save(account);
        return mapToDto(updatedAccount);
    }

    @Transactional
    public void deleteAccount(Long userId, Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

        if (!account.getUser().getId().equals(userId)) {
            throw new AccountAccessDeniedException("Нет доступа к этому счету");
        }

        if (!account.getTransactions().isEmpty()) {
            throw new IllegalStateException("Нельзя удалить счет с существующими транзакциями");
        }

        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalanceInBaseCurrency(Long userId, CurrencyCode baseCurrency) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Account> accounts = accountRepository.findByUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (Account account : accounts) {
            BigDecimal converted = currencyService.convert(
                    account.getCurrentBalance(),
                    account.getCurrency(),
                    baseCurrency,
                    LocalDate.now()
            );

            log.debug("Account {}: {} {} -> {} {}",
                    account.getName(),
                    account.getCurrentBalance(),
                    account.getCurrency(),
                    converted,
                    baseCurrency);

            total = total.add(converted);
        }

        log.info("Total balance for user {} in {}: {}", userId, baseCurrency, total);
        return total;
    }

    @Transactional(readOnly = true)
    public long countUserAccounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        return accountRepository.countByUser(user);
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .name(account.getName())
                .type(account.getType())
                .currency(account.getCurrency())
                .initialBalance(account.getInitialBalance())
                .currentBalance(account.getCurrentBalance())
                .description(account.getDescription())
                .build();
    }
}