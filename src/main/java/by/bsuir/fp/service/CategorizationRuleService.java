package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.RuleDto;
import by.bsuir.fp.exception.CategoryNotFoundException;
import by.bsuir.fp.exception.RuleNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.repository.CategorizationRuleRepository;
import by.bsuir.fp.repository.CategoryRepository;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategorizationRuleService {

    private final CategorizationRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public RuleDto createRule(Long userId, RuleDto ruleDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Category category = categoryRepository.findById(ruleDto.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

        if (!category.getUser().getId().equals(userId) && !category.getIsDefault()) {
            throw new SecurityException("Нет доступа к этой категории");
        }

        CategorizationRule rule = CategorizationRule.builder()
                .user(user)
                .category(category)
                .field(ruleDto.getField())
                .operator(ruleDto.getOperator())
                .value(ruleDto.getValue())
                .priority(ruleDto.getPriority() != null ? ruleDto.getPriority() : 0)
                .isActive(ruleDto.getIsActive() != null ? ruleDto.getIsActive() : true)
                .build();

        CategorizationRule savedRule = ruleRepository.save(rule);
        return mapToDto(savedRule);
    }

    @Transactional(readOnly = true)
    public RuleDto getRuleById(Long userId, Long ruleId) {
        CategorizationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Правило не найдено"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому правилу");
        }

        return mapToDto(rule);
    }

    @Transactional(readOnly = true)
    public List<RuleDto> getUserRules(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return ruleRepository.findByUser(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RuleDto> getActiveRules(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return ruleRepository.findByUserAndIsActiveTrueOrderByPriorityDesc(user).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RuleDto updateRule(Long userId, Long ruleId, RuleDto ruleDto) {
        CategorizationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Правило не найдено"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому правилу");
        }

        if (ruleDto.getCategoryId() != null && !rule.getCategory().getId().equals(ruleDto.getCategoryId())) {
            Category category = categoryRepository.findById(ruleDto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));
            rule.setCategory(category);
        }

        if (ruleDto.getField() != null) rule.setField(ruleDto.getField());
        if (ruleDto.getOperator() != null) rule.setOperator(ruleDto.getOperator());
        if (ruleDto.getValue() != null) rule.setValue(ruleDto.getValue());
        if (ruleDto.getPriority() != null) rule.setPriority(ruleDto.getPriority());
        if (ruleDto.getIsActive() != null) rule.setIsActive(ruleDto.getIsActive());

        return mapToDto(ruleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long userId, Long ruleId) {
        CategorizationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Правило не найдено"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому правилу");
        }

        ruleRepository.delete(rule);
    }

    @Transactional
    public void toggleRuleActive(Long userId, Long ruleId, Boolean active) {
        CategorizationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Правило не найдено"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому правилу");
        }

        rule.setIsActive(active);
        ruleRepository.save(rule);
    }

    @Transactional
    public void updatePriority(Long userId, Long ruleId, Integer priority) {
        CategorizationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundException("Правило не найдено"));

        if (!rule.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому правилу");
        }

        rule.setPriority(priority);
        ruleRepository.save(rule);
    }

    private RuleDto mapToDto(CategorizationRule rule) {
        return RuleDto.builder()
                .id(rule.getId())
                .categoryId(rule.getCategory().getId())
                .categoryName(rule.getCategory().getName())
                .field(rule.getField())
                .operator(rule.getOperator())
                .value(rule.getValue())
                .priority(rule.getPriority())
                .isActive(rule.getIsActive())
                .build();
    }
}