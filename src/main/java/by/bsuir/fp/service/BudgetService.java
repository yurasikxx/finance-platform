package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.controller.dto.BudgetLimitDto;
import by.bsuir.fp.exception.BudgetNotFoundException;
import by.bsuir.fp.exception.CategoryNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.*;
import by.bsuir.fp.model.enums.BudgetStatus;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetLimitRepository budgetLimitRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrencyService currencyService;

    @Transactional
    public BudgetDto createBudget(Long userId, BudgetCreateDto createDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        LocalDate startDate = LocalDate.of(createDto.getPeriodYear(), createDto.getPeriodMonth(), 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        budgetRepository.findByUserAndPeriodMonthAndPeriodYearAndStatus(
                        user, createDto.getPeriodMonth(), createDto.getPeriodYear(), BudgetStatus.ACTIVE)
                .ifPresent(b -> {
                    throw new IllegalStateException("Активный бюджет на этот период уже существует");
                });

        BigDecimal plannedIncomeInByn = createDto.getPlannedIncome();
        if (user.getDefaultCurrency() != CurrencyCode.BYN) {
            plannedIncomeInByn = currencyService.convert(
                    createDto.getPlannedIncome(),
                    user.getDefaultCurrency(),
                    CurrencyCode.BYN,
                    startDate
            );
        }

        Budget budget = Budget.builder()
                .user(user)
                .name(createDto.getName())
                .periodMonth(createDto.getPeriodMonth())
                .periodYear(createDto.getPeriodYear())
                .plannedIncome(plannedIncomeInByn)
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

                BigDecimal limitInByn = entry.getValue();
                if (user.getDefaultCurrency() != CurrencyCode.BYN) {
                    limitInByn = currencyService.convert(
                            entry.getValue(),
                            user.getDefaultCurrency(),
                            CurrencyCode.BYN,
                            startDate
                    );
                }

                BudgetLimit limit = BudgetLimit.builder()
                        .budget(savedBudget)
                        .category(category)
                        .limitAmount(limitInByn)
                        .spentAmount(BigDecimal.ZERO)
                        .build();

                budgetLimitRepository.save(limit);
                savedBudget.getLimits().add(limit);
            }
        }

        log.info("Budget created for user {}: {} - {}.{}", userId, createDto.getName(),
                createDto.getPeriodMonth(), createDto.getPeriodYear());

        return mapToDto(savedBudget, user.getDefaultCurrency());
    }

    @Transactional(readOnly = true)
    public BudgetDto getActiveBudget(Long userId, Integer month, Integer year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return budgetRepository.findByUserAndPeriodMonthAndPeriodYearAndStatus(user, month, year, BudgetStatus.ACTIVE)
                .map(budget -> mapToDto(budget, user.getDefaultCurrency()))
                .orElse(null);
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
                .map(budget -> mapToDto(budget, user.getDefaultCurrency()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetDto getBudgetById(Long userId, Long budgetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        return budgetRepository.findById(budgetId)
                .filter(budget -> budget.getUser().getId().equals(userId))
                .map(budget -> mapToDto(budget, user.getDefaultCurrency()))
                .orElse(null);
    }

    @Transactional
    public BudgetDto updateBudget(Long userId, Long budgetId, BudgetCreateDto updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        if (updateDto.getName() != null) {
            budget.setName(updateDto.getName());
        }
        if (updateDto.getPlannedIncome() != null) {
            budget.setPlannedIncome(updateDto.getPlannedIncome());
        }
        if (updateDto.getDescription() != null) {
            budget.setDescription(updateDto.getDescription());
        }

        if (updateDto.getCategoryLimits() != null) {
            budgetLimitRepository.deleteAll(budget.getLimits());
            budget.getLimits().clear();

            for (Map.Entry<Long, BigDecimal> entry : updateDto.getCategoryLimits().entrySet()) {
                if (entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                Category category = categoryRepository.findById(entry.getKey())
                        .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

                BudgetLimit limit = BudgetLimit.builder()
                        .budget(budget)
                        .category(category)
                        .limitAmount(entry.getValue())
                        .spentAmount(BigDecimal.ZERO)
                        .build();

                budgetLimitRepository.save(limit);
                budget.getLimits().add(limit);
            }
        }

        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Budget updated: {} for user {}", budgetId, userId);

        return mapToDto(updatedBudget, user.getDefaultCurrency());
    }

    @Transactional
    public void deleteBudget(Long userId, Long budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        budgetLimitRepository.deleteAll(budget.getLimits());
        budgetRepository.delete(budget);

        log.info("Budget deleted: {} for user {}", budgetId, userId);
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

        log.info("Budget completed: {} for user {}", budgetId, userId);
    }

    @Transactional(readOnly = true)
    public BudgetDto refreshBudgetStats(Long userId, Long budgetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BudgetNotFoundException("Бюджет не найден"));

        if (!budget.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этому бюджету");
        }

        return mapToDto(budget, user.getDefaultCurrency());
    }

    private BudgetDto mapToDto(Budget budget, CurrencyCode baseCurrency) {
        List<BudgetLimitDto> limitDtos = budget.getLimits().stream()
                .map(limit -> mapLimitToDto(limit, budget, baseCurrency))
                .collect(Collectors.toList());

        BigDecimal totalSpent = limitDtos.stream()
                .map(BudgetLimitDto::getSpentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal plannedIncomeInBase = budget.getPlannedIncome();
        if (baseCurrency != CurrencyCode.BYN) {
            plannedIncomeInBase = currencyService.convert(
                    budget.getPlannedIncome(),
                    CurrencyCode.BYN,
                    baseCurrency,
                    budget.getStartDate()
            );
        }

        BigDecimal remainingBudget = plannedIncomeInBase.subtract(totalSpent);

        BigDecimal totalLimit = limitDtos.stream()
                .map(BudgetLimitDto::getLimitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int progressPercentage = plannedIncomeInBase.compareTo(BigDecimal.ZERO) > 0 ?
                totalSpent.multiply(BigDecimal.valueOf(100))
                        .divide(plannedIncomeInBase, 0, RoundingMode.HALF_UP)
                        .intValue() : 0;

        return BudgetDto.builder()
                .id(budget.getId())
                .name(budget.getName())
                .periodMonth(budget.getPeriodMonth())
                .periodYear(budget.getPeriodYear())
                .plannedIncome(plannedIncomeInBase)
                .status(budget.getStatus())
                .startDate(budget.getStartDate())
                .endDate(budget.getEndDate())
                .description(budget.getDescription())
                .limits(limitDtos)
                .totalSpent(totalSpent)
                .totalLimit(totalLimit)
                .remainingBudget(remainingBudget)
                .progressPercentage(progressPercentage)
                .build();
    }

    private BudgetLimitDto mapLimitToDto(BudgetLimit limit, Budget budget, CurrencyCode baseCurrency) {
        BigDecimal spentInBase = getSpentForCategoryInBaseCurrency(
                budget.getUser().getId(),
                limit.getCategory().getId(),
                budget.getStartDate(),
                budget.getEndDate(),
                baseCurrency
        );

        BigDecimal limitInBase;
        if (baseCurrency == CurrencyCode.BYN) {
            limitInBase = limit.getLimitAmount();
        } else {
            limitInBase = currencyService.convert(
                    limit.getLimitAmount(),
                    CurrencyCode.BYN,
                    baseCurrency,
                    LocalDate.now()
            );
        }

        if (limit.getSpentAmount().compareTo(spentInBase) != 0) {
            limit.setSpentAmount(spentInBase);
            budgetLimitRepository.save(limit);
        }

        BigDecimal remaining = limitInBase.subtract(spentInBase);

        int progressPercentage = limitInBase.compareTo(BigDecimal.ZERO) > 0 ?
                spentInBase.multiply(BigDecimal.valueOf(100))
                        .divide(limitInBase, 0, RoundingMode.HALF_UP)
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
                .limitAmount(limitInBase)
                .spentAmount(spentInBase)
                .remainingAmount(remaining)
                .progressPercentage(progressPercentage)
                .progressStatus(progressStatus)
                .build();
    }

    private BigDecimal getSpentForCategoryInBaseCurrency(Long userId, Long categoryId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         CurrencyCode baseCurrency) {
        List<Transaction> transactions = transactionRepository
                .findByUserAndCategoryAndTransactionDateBetween(
                        User.builder().id(userId).build(),
                        Category.builder().id(categoryId).build(),
                        startDate,
                        endDate
                );

        return transactions.stream()
                .filter(tx -> tx.getType() == TransactionType.EXPENSE)
                .map(tx -> currencyService.convertTransaction(tx, baseCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}