package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.exception.AccountNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.AccountRepository;
import by.bsuir.fp.repository.CategoryRepository;
import by.bsuir.fp.repository.TransactionRepository;
import by.bsuir.fp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Account testAccount;
    private Category testCategory;
    private TransactionDto testTransactionDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setDefaultCurrency(CurrencyCode.USD);

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setName("Тестовый счет");
        testAccount.setCurrency(CurrencyCode.USD);
        testAccount.setCurrentBalance(new BigDecimal("1000.00"));

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Продукты");
        testCategory.setColor("#dc3545");

        testTransactionDto = TransactionDto.builder()
                .accountId(1L)
                .categoryId(1L)
                .amount(new BigDecimal("150.50"))
                .type(TransactionType.EXPENSE)
                .transactionDate(LocalDate.now())
                .description("Тестовая транзакция")
                .build();
    }

    @Test
    void createTransaction_WithValidData_ShouldSaveTransaction() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currencyService.convertTransaction(any(), any())).thenReturn(new BigDecimal("150.50"));

        TransactionDto result = transactionService.createTransaction(1L, testTransactionDto);

        assertNotNull(result);
        assertEquals(new BigDecimal("150.50"), result.getAmount());
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals("Продукты", result.getCategoryName());

        verify(accountRepository).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WithNonExistentUser_ShouldThrowException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> transactionService.createTransaction(999L, testTransactionDto));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_WithNonExistentAccount_ShouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        testTransactionDto.setAccountId(999L);

        assertThrows(AccountNotFoundException.class, () -> transactionService.createTransaction(1L, testTransactionDto));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_ShouldUpdateAccountBalance() {
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUser(testUser);
        transaction.setAccount(testAccount);
        transaction.setAmount(new BigDecimal("150.50"));
        transaction.setType(TransactionType.EXPENSE);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(1L, 1L);

        verify(accountRepository).save(argThat(account ->
                account.getCurrentBalance().compareTo(new BigDecimal("1150.50")) == 0
        ));
        verify(transactionRepository).delete(transaction);
    }

    @Test
    void getMonthlyStatsInBaseCurrency_ShouldCalculateCorrectly() {
        Transaction income = new Transaction();
        income.setUser(testUser);
        income.setAccount(testAccount);
        income.setType(TransactionType.INCOME);
        income.setAmount(new BigDecimal("1000.00"));
        income.setTransactionDate(LocalDate.now());

        Transaction expense = new Transaction();
        expense.setUser(testUser);
        expense.setAccount(testAccount);
        expense.setType(TransactionType.EXPENSE);
        expense.setAmount(new BigDecimal("300.00"));
        expense.setTransactionDate(LocalDate.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(eq(testUser), any(), any()))
                .thenReturn(Arrays.asList(income, expense));
        when(currencyService.convertTransaction(any(Transaction.class), eq(CurrencyCode.USD)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    return tx.getAmount();
                });

        var stats = transactionService.getMonthlyStatsInBaseCurrency(1L, CurrencyCode.USD);

        assertEquals(new BigDecimal("1000.00"), stats.get("income"));
        assertEquals(new BigDecimal("300.00"), stats.get("expense"));
        assertEquals(new BigDecimal("700.00"), stats.get("balance"));
    }
}