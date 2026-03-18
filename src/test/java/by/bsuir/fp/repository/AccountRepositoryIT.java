package by.bsuir.fp.repository;

import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.AccountType;
import by.bsuir.fp.model.enums.CurrencyCode;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryIT {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash123");
        testUser.setDefaultCurrency(CurrencyCode.USD);
        entityManager.persist(testUser);

        account1 = new Account();
        account1.setUser(testUser);
        account1.setName("Основная карта");
        account1.setType(AccountType.BANK_CARD);
        account1.setCurrency(CurrencyCode.USD);
        account1.setInitialBalance(new BigDecimal("1000.00"));
        account1.setCurrentBalance(new BigDecimal("1500.00"));

        account2 = new Account();
        account2.setUser(testUser);
        account2.setName("Наличные");
        account2.setType(AccountType.CASH);
        account2.setCurrency(CurrencyCode.BYN);
        account2.setInitialBalance(new BigDecimal("500.00"));
        account2.setCurrentBalance(new BigDecimal("300.00"));
    }

    @Test
    void saveAccount_ShouldPersistAccount() {
        Account saved = accountRepository.save(account1);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Основная карта");
        assertThat(saved.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void findByUser_ShouldReturnUserAccounts() {
        entityManager.persist(account1);
        entityManager.persist(account2);
        entityManager.flush();

        List<Account> accounts = accountRepository.findByUser(testUser);

        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(Account::getName)
                .containsExactlyInAnyOrder("Основная карта", "Наличные");
    }

    @Test
    void getTotalBalanceByUser_ShouldCalculateSum() {
        entityManager.persist(account1);
        entityManager.persist(account2);
        entityManager.flush();

        BigDecimal total = accountRepository.getTotalBalanceByUser(testUser);
        assertThat(total).isEqualByComparingTo(new BigDecimal("1800.00"));
    }

    @Test
    void deleteAccount_ShouldRemoveAccount() {
        Account saved = entityManager.persist(account1);
        entityManager.flush();

        accountRepository.deleteById(saved.getId());

        List<Account> accounts = accountRepository.findByUser(testUser);
        assertThat(accounts).isEmpty();
    }
}