package by.bsuir.fp.repository;

import by.bsuir.fp.model.Budget;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.BudgetStatus;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BudgetRepositoryIT {

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
    private BudgetRepository budgetRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Budget budget1;
    private Budget budget2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hash123");
        testUser.setDefaultCurrency(CurrencyCode.USD);
        entityManager.persist(testUser);

        Category testCategory = new Category();
        testCategory.setUser(testUser);
        testCategory.setName("Продукты");
        testCategory.setType(TransactionType.EXPENSE);
        testCategory.setColor("#dc3545");
        entityManager.persist(testCategory);

        budget1 = new Budget();
        budget1.setUser(testUser);
        budget1.setName("Бюджет марта");
        budget1.setPeriodMonth(3);
        budget1.setPeriodYear(2026);
        budget1.setPlannedIncome(new BigDecimal("2000.00"));
        budget1.setStatus(BudgetStatus.ACTIVE);
        budget1.setStartDate(LocalDate.of(2026, 3, 1));
        budget1.setEndDate(LocalDate.of(2026, 3, 31));

        budget2 = new Budget();
        budget2.setUser(testUser);
        budget2.setName("Бюджет апреля");
        budget2.setPeriodMonth(4);
        budget2.setPeriodYear(2026);
        budget2.setPlannedIncome(new BigDecimal("2500.00"));
        budget2.setStatus(BudgetStatus.DRAFT);
        budget2.setStartDate(LocalDate.of(2026, 4, 1));
        budget2.setEndDate(LocalDate.of(2026, 4, 30));
    }

    @Test
    void saveBudget_ShouldPersistBudget() {
        Budget saved = budgetRepository.save(budget1);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Бюджет марта");
        assertThat(saved.getStatus()).isEqualTo(BudgetStatus.ACTIVE);
    }

    @Test
    void findByUser_ShouldReturnUserBudgets() {
        entityManager.persist(budget1);
        entityManager.persist(budget2);
        entityManager.flush();

        List<Budget> budgets = budgetRepository.findByUser(testUser);

        assertThat(budgets).hasSize(2);
        assertThat(budgets).extracting(Budget::getName)
                .containsExactlyInAnyOrder("Бюджет марта", "Бюджет апреля");
    }

    @Test
    void findByUserAndPeriodMonthAndPeriodYearAndStatus_ShouldReturnBudget() {
        entityManager.persist(budget1);
        entityManager.flush();

        Optional<Budget> found = budgetRepository.findByUserAndPeriodMonthAndPeriodYearAndStatus(
                testUser, 3, 2026, BudgetStatus.ACTIVE);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Бюджет марта");
    }

    @Test
    void findByUserAndPeriodYear_ShouldFilterByYear() {
        entityManager.persist(budget1);
        entityManager.persist(budget2);
        entityManager.flush();

        List<Budget> budgets = budgetRepository.findByUserAndPeriodYear(testUser, 2026);

        assertThat(budgets).hasSize(2);
    }


    @Test
    void deleteBudget_ShouldRemoveBudget() {
        Budget saved = entityManager.persist(budget1);
        entityManager.flush();

        budgetRepository.deleteById(saved.getId());

        List<Budget> budgets = budgetRepository.findByUser(testUser);
        assertThat(budgets).isEmpty();
    }
}