package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.RuleDto;
import by.bsuir.fp.exception.RuleNotFoundException;
import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.RuleOperator;
import by.bsuir.fp.repository.CategorizationRuleRepository;
import by.bsuir.fp.repository.CategoryRepository;
import by.bsuir.fp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategorizationRuleServiceTest {

    @Mock
    private CategorizationRuleRepository ruleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategorizationRuleService ruleService;

    private User testUser;
    private Category testCategory;
    private CategorizationRule testRule;
    private RuleDto testRuleDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Транспорт");
        testCategory.setUser(testUser);

        testRule = new CategorizationRule();
        testRule.setId(1L);
        testRule.setUser(testUser);
        testRule.setCategory(testCategory);
        testRule.setField("description");
        testRule.setOperator(RuleOperator.CONTAINS);
        testRule.setValue("АЗС");
        testRule.setPriority(10);
        testRule.setIsActive(true);

        testRuleDto = RuleDto.builder()
                .categoryId(1L)
                .field("description")
                .operator(RuleOperator.CONTAINS)
                .value("АЗС")
                .priority(10)
                .isActive(true)
                .build();
    }

    @Test
    void createRule_WithValidData_ShouldSaveRule() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(ruleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RuleDto result = ruleService.createRule(1L, testRuleDto);

        assertNotNull(result);
        assertEquals("description", result.getField());
        assertEquals(RuleOperator.CONTAINS, result.getOperator());
        assertEquals("АЗС", result.getValue());

        verify(ruleRepository).save(any(CategorizationRule.class));
    }

    @Test
    void getUserRules_ShouldReturnList() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ruleRepository.findByUser(testUser))
                .thenReturn(Collections.singletonList(testRule));

        List<RuleDto> results = ruleService.getUserRules(1L);

        assertEquals(1, results.size());
        assertEquals("АЗС", results.getFirst().getValue());
    }

    @Test
    void getActiveRules_ShouldReturnOnlyActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(ruleRepository.findByUserAndIsActiveTrueOrderByPriorityDesc(testUser))
                .thenReturn(Collections.singletonList(testRule));

        List<RuleDto> results = ruleService.getActiveRules(1L);

        assertEquals(1, results.size());
        assertTrue(results.getFirst().getIsActive());
    }

    @Test
    void updateRule_ShouldUpdateFields() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(ruleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        testRuleDto.setValue("Бензин");
        testRuleDto.setPriority(5);

        RuleDto result = ruleService.updateRule(1L, 1L, testRuleDto);

        assertEquals("Бензин", result.getValue());
        assertEquals(5, result.getPriority());
    }

    @Test
    void deleteRule_WithExistingRule_ShouldDelete() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        ruleService.deleteRule(1L, 1L);

        verify(ruleRepository).delete(testRule);
    }

    @Test
    void deleteRule_WithNonExistentRule_ShouldThrowException() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuleNotFoundException.class, () -> ruleService.deleteRule(1L, 999L));

        verify(ruleRepository, never()).delete(any());
    }

    @Test
    void toggleRuleActive_ShouldChangeStatus() {
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(ruleRepository.save(any(CategorizationRule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ruleService.toggleRuleActive(1L, 1L, false);

        assertFalse(testRule.getIsActive());
        verify(ruleRepository).save(testRule);
    }
}