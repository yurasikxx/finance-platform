package by.bsuir.fp.repository;

import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.AccountType;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransactionRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash123");
        testUser.setDefaultCurrency(CurrencyCode.USD);
        entityManager.persist(testUser);

        Account testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setName("Тестовый счет");
        testAccount.setType(AccountType.BANK_CARD);
        testAccount.setCurrency(CurrencyCode.USD);
        testAccount.setCurrentBalance(BigDecimal.ZERO);
        testAccount.setInitialBalance(BigDecimal.ZERO);
        entityManager.persist(testAccount);

        Category testCategory = new Category();
        testCategory.setUser(testUser);
        testCategory.setName("Продукты");
        testCategory.setType(TransactionType.EXPENSE);
        testCategory.setColor("#dc3545");
        entityManager.persist(testCategory);

        transaction1 = new Transaction();
        transaction1.setUser(testUser);
        transaction1.setAccount(testAccount);
        transaction1.setCategory(testCategory);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setType(TransactionType.EXPENSE);
        transaction1.setTransactionDate(LocalDate.of(2026, 3, 1));
        transaction1.setDescription("Тест 1");

        transaction2 = new Transaction();
        transaction2.setUser(testUser);
        transaction2.setAccount(testAccount);
        transaction2.setCategory(testCategory);
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setType(TransactionType.INCOME);
        transaction2.setTransactionDate(LocalDate.of(2026, 3, 15));
        transaction2.setDescription("Тест 2");
    }

    @Test
    void saveTransaction_ShouldPersistTransaction() {
        Transaction saved = transactionRepository.save(transaction1);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(saved.getType()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void findByUserAndDateBetween_ShouldReturnTransactionsInPeriod() {
        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.flush();

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        List<Transaction> results = transactionRepository
                .findByUserAndTransactionDateBetween(testUser, start, end);

        assertThat(results).hasSize(2);
    }

    @Test
    void countByUser_ShouldReturnCorrectCount() {
        entityManager.persist(transaction1);
        entityManager.persist(transaction2);
        entityManager.flush();

        long count = transactionRepository.countByUser(testUser);

        assertThat(count).isEqualTo(2);
    }
}