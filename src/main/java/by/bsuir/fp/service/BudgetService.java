package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.controller.dto.BudgetLimitDto;
import by.bsuir.fp.exception.BudgetNotFoundException;
import by.bsuir.fp.exception.CategoryNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Budget;
import by.bsuir.fp.model.BudgetLimit;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.BudgetStatus;
import by.bsuir.fp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BudgetDto createBudget(Long userId, BudgetCreateDto createDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        LocalDate startDate = LocalDate.of(createDto.getPeriodYear(), createDto.getPeriodMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        Budget budget = Budget.builder()
                .user(user)
                .name(createDto.getName())
                .periodMonth(createDto.getPeriodMonth())
                .periodYear(createDto.getPeriodYear())
                .plannedIncome(createDto.getPlannedIncome())
                .status(BudgetStatus.ACTIVE)
                .startDate(startDate)
                .endDate(endDate)
                .description(createDto.getDescription())
                .limits(new ArrayList<>())
                .build();

        Budget savedBudget = budgetRepository.save(budget);

        if (createDto.getCategoryLimits() != null) {
            for (Map.Entry<Long, BigDecimal> entry : createDto.getCategoryLimits().entrySet()) {
                if (entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                Category category = categoryRepository.findById(entry.getKey())
                        .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

                if (!category.getUser().getId().equals(userId) && !category.getIsDefault()) {
                    throw new SecurityException("Нет доступа к категории: " + category.getName());
                }

                BudgetLimit limit = BudgetLimit.builder()
                        .budget(savedBudget)
                        .category(category)
                        .limitAmount(entry.getValue())
                        .spentAmount(calculateSpentForCategory(user, category, startDate, endDate))
                        .build();

                budgetLimitRepository.save(limit);
                savedBudget.getLimits().add(limit);
            }
        }

        return mapToDto(savedBudget);
    }

    private BigDecimal calculateSpentForCategory(User user, Category category, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.getTotalExpenseByCategoryAndPeriod(user, category, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public BudgetDto getActiveBudget(Long userId, Integer month, Integer year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Budget budget = budgetRepository
                .findByUserAndPeriodMonthAndPeriodYearAndStatus(user, month, year, BudgetStatus.ACTIVE)
                .orElse(null);

        if (budget == null) {
            return null;
        }

        return mapToDto(budget);
    }

    @Transactional(readOnly = true)
    public List<BudgetDto> getUserBudgets(Long userId, Integer year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Budget> budgets;
        if (year != null) {
            budgets = budgetRepository.findByUserAndPeriodYear(user, year);
        } else {
            budgets = budgetRepository.findByUser(user);
        }

        return budgets.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetDto updateBudget(Long userId, Long budgetId, BudgetCreateDto updateDto) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        if (updateDto.getName() != null) budget.setName(updateDto.getName());
        if (updateDto.getPlannedIncome() != null) budget.setPlannedIncome(updateDto.getPlannedIncome());
        if (updateDto.getDescription() != null) budget.setDescription(updateDto.getDescription());

        if (updateDto.getCategoryLimits() != null) {
            for (Map.Entry<Long, BigDecimal> entry : updateDto.getCategoryLimits().entrySet()) {
                Category category = categoryRepository.findById(entry.getKey())
                        .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

                BudgetLimit limit = budgetLimitRepository
                        .findByBudgetAndCategory(budget, category)
                        .orElse(BudgetLimit.builder()
                                .budget(budget)
                                .category(category)
                                .build());

                limit.setLimitAmount(entry.getValue());
                budgetLimitRepository.save(limit);

                if (!budget.getLimits().contains(limit)) {
                    budget.getLimits().add(limit);
                }
            }
        }

        Budget updatedBudget = budgetRepository.save(budget);
        return mapToDto(updatedBudget);
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        budgetRepository.delete(budget);
    }

    @Transactional
    public void completeBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        budget.setStatus(BudgetStatus.COMPLETED);
        budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public BudgetDto refreshBudgetStats(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        for (BudgetLimit limit : budget.getLimits()) {
            BigDecimal spent = transactionRepository.getTotalExpenseByCategoryAndPeriod(
                    budget.getUser(),
                    limit.getCategory(),
                    budget.getStartDate(),
                    budget.getEndDate()
            );
            limit.setSpentAmount(spent != null ? spent : BigDecimal.ZERO);
            budgetLimitRepository.save(limit);
        }

        return mapToDto(budget);
    }

    public Optional<BudgetDto> getBudgetById(Long userId, Long budgetId) {
        return budgetRepository.findById(budgetId)
                .filter(budget -> budget.getUser().getId().equals(userId))
                .map(this::mapToDto);
    }

    private BudgetDto mapToDto(Budget budget) {
        List<BudgetLimitDto> limitDtos = budget.getLimits().stream()
                .map(this::mapLimitToDto)
                .collect(Collectors.toList());

        BigDecimal totalSpent = limitDtos.stream()
                .map(BudgetLimitDto::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLimit = limitDtos.stream()
                .map(BudgetLimitDto::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int progressPercentage = totalLimit.compareTo(BigDecimal.ZERO) > 0 ?
                totalSpent.multiply(BigDecimal.valueOf(100))
                        .divide(totalLimit, 0, RoundingMode.HALF_UP)
                        .intValue() : 0;

        return BudgetDto.builder()
                .id(budget.getId())
                .name(budget.getName())
                .periodMonth(budget.getPeriodMonth())
                .periodYear(budget.getPeriodYear())
                .plannedIncome(budget.getPlannedIncome())
                .status(budget.getStatus())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .description(budget.getDescription())
                .limits(limitDtos)
                .totalSpent(totalSpent)
                .totalLimit(totalLimit)
                .remainingBudget(totalLimit.subtract(totalSpent))
                .progressPercentage(progressPercentage)
                .build();
    }

    private BudgetLimitDto mapLimitToDto(BudgetLimit limit) {
        BigDecimal spent = limit.getSpentAmount() != null ? limit.getSpentAmount() : BigDecimal.ZERO;
        BigDecimal remaining = limit.getLimitAmount().subtract(spent);

        int progressPercentage = limit.getLimitAmount().compareTo(BigDecimal.ZERO) > 0 ?
                spent.multiply(BigDecimal.valueOf(100))
                        .divide(limit.getLimitAmount(), 0, RoundingMode.HALF_UP)
                        .intValue() : 0;

        String progressStatus = "normal";
        if (progressPercentage >= 100) {
            progressStatus = "danger";
        } else if (progressPercentage >= 80) {
            progressStatus = "warning";
        }

        return BudgetLimitDto.builder()
                .id(limit.getId())
                .categoryId(limit.getCategory().getId())
                .categoryName(limit.getCategory().getName())
                .categoryColor(limit.getCategory().getColor())
                .limitAmount(limit.getLimitAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .progressPercentage(progressPercentage)
                .progressStatus(progressStatus)
                .build();
    }
}