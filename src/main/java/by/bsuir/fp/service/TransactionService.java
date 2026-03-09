package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.exception.AccountNotFoundException;
import by.bsuir.fp.exception.CategoryNotFoundException;
import by.bsuir.fp.exception.TransactionNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.*;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationRuleRepository ruleRepository;

    private final DescriptionCategorizationStrategy descriptionStrategy;
    private final AmountCategorizationStrategy amountStrategy;

    @Transactional
    public TransactionDto createTransaction(Long userId, TransactionDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Account account = accountRepository.findById(dto.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

        if (!account.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому счету");
        }

        Transaction transaction = Transaction.builder()
                .user(user)
                .account(account)
                .type(dto.getType())
                .amount(dto.getAmount())
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : LocalDate.now())
                .description(dto.getDescription())
                .isCategorized(false)
                .isReconciled(false)
                .build();

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));
            transaction.setCategory(category);
            transaction.setIsCategorized(true);
        } else {
            applyCategorizationRules(transaction, user);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);

        account.setCurrentBalance(updateBalance(account.getCurrentBalance(),
                transaction.getAmount(), transaction.getType()));
        accountRepository.save(account);

        return mapToDto(savedTransaction);
    }

    private void applyCategorizationRules(Transaction transaction, User user) {
        List<CategorizationRule> rules = ruleRepository
                .findByUserAndIsActiveTrueOrderByPriorityDesc(user);

        Map<String, CategorizationStrategy> strategyMap = new HashMap<>();
        strategyMap.put("description", descriptionStrategy);
        strategyMap.put("amount", amountStrategy);

        for (CategorizationRule rule : rules) {
            CategorizationStrategy strategy = strategyMap.get(rule.getField());
            if (strategy != null && strategy.matches(transaction, rule)) {
                transaction.setCategory(rule.getCategory());
                transaction.setIsCategorized(true);
                break;
            }
        }
    }

    private BigDecimal updateBalance(BigDecimal currentBalance, BigDecimal amount, TransactionType type) {
        return switch (type) {
            case INCOME -> currentBalance.add(amount);
            case EXPENSE -> currentBalance.subtract(amount);
            case TRANSFER -> currentBalance; // обрабатывается отдельно
        };
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionById(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой транзакции");
        }

        return mapToDto(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactions(Long userId, TransactionFilterDto filter) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Pageable pageable = PageRequest.of(
                filter.getPage() != null ? filter.getPage() : 0,
                filter.getSize() != null ? filter.getSize() : 20,
                Sort.by("transactionDate").descending().and(Sort.by("id").descending())
        );

        List<Specification<Transaction>> specs = new ArrayList<>();
        specs.add(TransactionSpecification.byUser(user));

        if (filter.getAccountId() != null) {
            specs.add(TransactionSpecification.byAccountId(filter.getAccountId()));
        }
        if (filter.getCategoryId() != null) {
            specs.add(TransactionSpecification.byCategoryId(filter.getCategoryId()));
        }
        if (filter.getType() != null) {
            specs.add(TransactionSpecification.byType(filter.getType()));
        }
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            specs.add(TransactionSpecification.byDateBetween(filter.getFromDate(), filter.getToDate()));
        }
        if (filter.getSearchText() != null && !filter.getSearchText().trim().isEmpty()) {
            specs.add(TransactionSpecification.bySearchText(filter.getSearchText()));
        }

        Specification<Transaction> spec = Specification.allOf(specs);

        return transactionRepository.findAll(spec, pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, TransactionDto dto) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой транзакции");
        }

        BigDecimal oldAmount = transaction.getAmount();
        TransactionType oldType = transaction.getType();
        Account oldAccount = transaction.getAccount();

        if (dto.getAccountId() != null && !dto.getAccountId().equals(transaction.getAccount().getId())) {
            Account newAccount = accountRepository.findById(dto.getAccountId())
                    .orElseThrow(() -> new AccountNotFoundException("Счет не найден"));

            if (!newAccount.getUser().getId().equals(userId)) {
                throw new SecurityException("Нет доступа к этому счету");
            }

            oldAccount.setCurrentBalance(reverseBalanceUpdate(oldAccount.getCurrentBalance(), oldAmount, oldType));
            accountRepository.save(oldAccount);

            transaction.setAccount(newAccount);
        }

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));
            transaction.setCategory(category);
            transaction.setIsCategorized(true);
        } else {
            transaction.setCategory(null);
            transaction.setIsCategorized(false);
            applyCategorizationRules(transaction, transaction.getUser());
        }

        transaction.setAmount(dto.getAmount());
        transaction.setType(dto.getType());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setDescription(dto.getDescription());

        Transaction updatedTransaction = transactionRepository.save(transaction);

        Account currentAccount = updatedTransaction.getAccount();
        currentAccount.setCurrentBalance(updateBalance(
                currentAccount.getCurrentBalance(),
                updatedTransaction.getAmount(),
                updatedTransaction.getType()));
        accountRepository.save(currentAccount);

        return mapToDto(updatedTransaction);
    }

    private BigDecimal reverseBalanceUpdate(BigDecimal currentBalance, BigDecimal amount, TransactionType type) {
        return switch (type) {
            case INCOME -> currentBalance.subtract(amount);
            case EXPENSE -> currentBalance.add(amount);
            case TRANSFER -> currentBalance;
        };
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой транзакции");
        }

        Account account = transaction.getAccount();
        account.setCurrentBalance(reverseBalanceUpdate(
                account.getCurrentBalance(),
                transaction.getAmount(),
                transaction.getType()));
        accountRepository.save(account);

        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getUncategorizedTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return transactionRepository.findByUserAndIsCategorizedFalse(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionDto categorizeTransaction(Long userId, Long transactionId, Long categoryId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Транзакция не найдена"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой транзакции");
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

        transaction.setCategory(category);
        transaction.setIsCategorized(true);

        return mapToDto(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getDailyStats(Long userId, LocalDate fromDate, LocalDate toDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        BigDecimal income = transactionRepository.getTotalIncomeByPeriod(user, fromDate, toDate);
        BigDecimal expense = transactionRepository.getTotalExpenseByCategoryAndPeriod(
                user, null, fromDate, toDate);

        Map<String, BigDecimal> stats = new HashMap<>();
        stats.put("income", income);
        stats.put("expense", expense);
        stats.put("balance", income.subtract(expense));

        return stats;
    }

    private TransactionDto mapToDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .accountId(transaction.getAccount().getId())
                .categoryId(transaction.getCategory() != null ? transaction.getCategory().getId() : null)
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .isCategorized(transaction.getIsCategorized())
                .build();
    }
}