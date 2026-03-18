package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.exception.BudgetNotFoundException;
import by.bsuir.fp.model.Budget;
import by.bsuir.fp.model.BudgetLimit;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.BudgetStatus;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private BudgetLimitRepository budgetLimitRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;  // ЭТО БЫЛО ПРОПУЩЕНО!

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Category testCategory;
    private Budget testBudget;
    private BudgetCreateDto testCreateDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setDefaultCurrency(CurrencyCode.USD);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Продукты");
        testCategory.setColor("#dc3545");
        testCategory.setUser(testUser);

        testBudget = new Budget();
        testBudget.setId(1L);
        testBudget.setUser(testUser);
        testBudget.setName("Бюджет марта");
        testBudget.setPeriodMonth(3);
        testBudget.setPeriodYear(2026);
        testBudget.setPlannedIncome(new BigDecimal("2000.00"));
        testBudget.setStatus(BudgetStatus.ACTIVE);
        testBudget.setStartDate(LocalDate.of(2026, 3, 1));
        testBudget.setEndDate(LocalDate.of(2026, 3, 31));
        testBudget.setLimits(new ArrayList<>());

        BudgetLimit testLimit = new BudgetLimit();
        testLimit.setId(1L);
        testLimit.setBudget(testBudget);
        testLimit.setCategory(testCategory);
        testLimit.setLimitAmount(new BigDecimal("500.00"));
        testLimit.setSpentAmount(new BigDecimal("0.00"));

        testBudget.getLimits().add(testLimit);

        Map<Long, BigDecimal> limits = new HashMap<>();
        limits.put(1L, new BigDecimal("500.00"));

        testCreateDto = new BudgetCreateDto();
        testCreateDto.setName("Бюджет марта");
        testCreateDto.setPeriodMonth(3);
        testCreateDto.setPeriodYear(2026);
        testCreateDto.setPlannedIncome(new BigDecimal("2000.00"));
        testCreateDto.setCategoryLimits(limits);
    }

    @Test
    void createBudget_WithValidData_ShouldSaveBudget() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currencyService.convert(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByUserAndCategoryAndTransactionDateBetween(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BudgetDto result = budgetService.createBudget(1L, testCreateDto);

        assertNotNull(result);
        assertEquals("Бюджет марта", result.getName());
        assertEquals(3, result.getPeriodMonth());
        assertEquals(2026, result.getPeriodYear());
        assertEquals(new BigDecimal("2000.00"), result.getPlannedIncome());

        verify(budgetRepository).save(any(Budget.class));
        verify(budgetLimitRepository, atLeastOnce()).save(any(BudgetLimit.class));
    }

    @Test
    void getActiveBudget_WhenExists_ShouldReturnBudget() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserAndPeriodMonthAndPeriodYearAndStatus(
                testUser, 3, 2026, BudgetStatus.ACTIVE))
                .thenReturn(Optional.of(testBudget));
        when(currencyService.convert(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByUserAndCategoryAndTransactionDateBetween(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BudgetDto result = budgetService.getActiveBudget(1L, 3, 2026);

        assertNotNull(result);
        assertEquals("Бюджет марта", result.getName());
    }

    @Test
    void getActiveBudget_WhenNotExists_ShouldReturnNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUserAndPeriodMonthAndPeriodYearAndStatus(
                testUser, 3, 2026, BudgetStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BudgetDto result = budgetService.getActiveBudget(1L, 3, 2026);

        assertNull(result);
    }

    @Test
    void getUserBudgets_ShouldReturnList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findByUser(testUser))
                .thenReturn(Collections.singletonList(testBudget));
        when(currencyService.convert(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByUserAndCategoryAndTransactionDateBetween(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<BudgetDto> results = budgetService.getUserBudgets(1L, null);

        assertEquals(1, results.size());
        assertEquals("Бюджет марта", results.getFirst().getName());
    }

    @Test
    void updateBudget_ShouldUpdateFields() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currencyService.convert(any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.findByUserAndCategoryAndTransactionDateBetween(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        testCreateDto.setName("Обновленный бюджет");
        testCreateDto.setPlannedIncome(new BigDecimal("2500.00"));

        BudgetDto result = budgetService.updateBudget(1L, 1L, testCreateDto);

        assertEquals("Обновленный бюджет", result.getName());
        assertEquals(new BigDecimal("2500.00"), result.getPlannedIncome());
    }

    @Test
    void deleteBudget_ShouldDelete() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));

        budgetService.deleteBudget(1L, 1L);

        verify(budgetLimitRepository).deleteAll(testBudget.getLimits());
        verify(budgetRepository).delete(testBudget);
    }

    @Test
    void deleteBudget_WithNonExistentBudget_ShouldThrowException() {
        when(budgetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BudgetNotFoundException.class, () -> budgetService.deleteBudget(1L, 999L));

        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void completeBudget_ShouldChangeStatus() {
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(testBudget));
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        budgetService.completeBudget(1L, 1L);

        assertEquals(BudgetStatus.COMPLETED, testBudget.getStatus());
    }
}