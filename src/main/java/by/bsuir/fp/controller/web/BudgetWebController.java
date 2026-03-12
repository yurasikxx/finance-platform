package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.BudgetService;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetWebController {

    private final SecurityService securityService;
    private final UserService userService;
    private final BudgetService budgetService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer year,
                       Model model,
                       HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);

        if (year == null) {
            year = YearMonth.now().getYear();
        }

        List<BudgetDto> budgets = budgetService.getUserBudgets(userId, year);

        model.addAttribute("budgets", budgets);
        model.addAttribute("selectedYear", year);
        model.addAttribute("years", List.of(year - 1, year, year + 1));
        model.addAttribute("baseCurrency", user.getDefaultCurrency());
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/list";
    }

    @GetMapping("/active")
    public String active(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);
        YearMonth current = YearMonth.now();

        BudgetDto activeBudget = budgetService.getActiveBudget(
                userId,
                current.getMonthValue(),
                current.getYear()
        );

        model.addAttribute("budget", activeBudget);
        model.addAttribute("baseCurrency", user.getDefaultCurrency());
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/active";
    }

    @GetMapping("/create")
    public String createForm(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);
        YearMonth current = YearMonth.now();

        BudgetCreateDto budgetCreate = new BudgetCreateDto();
        budgetCreate.setPeriodMonth(current.getMonthValue());
        budgetCreate.setPeriodYear(current.getYear());

        var categories = categoryService.getUserCategories(userId, TransactionType.EXPENSE);

        model.addAttribute("budget", budgetCreate);
        model.addAttribute("categories", categories);
        model.addAttribute("months", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        model.addAttribute("currentYear", current.getYear());
        model.addAttribute("baseCurrency", user.getDefaultCurrency());
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute BudgetCreateDto budgetCreate,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();

            if (budgetCreate.getCategoryLimits() != null) {
                budgetCreate.getCategoryLimits().entrySet().removeIf(
                        entry -> entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) <= 0
                );
            }

            budgetService.createBudget(userId, budgetCreate);
            redirectAttributes.addFlashAttribute("success", "Бюджет успешно создан");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Error creating budget", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка создания бюджета: " + e.getMessage());
        }

        return "redirect:/budgets/active";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);

        BudgetDto budget = budgetService.getBudgetById(userId, id);
        if (budget == null) {
            return "redirect:/budgets";
        }

        BudgetCreateDto budgetCreate = new BudgetCreateDto();
        budgetCreate.setName(budget.getName());
        budgetCreate.setPeriodMonth(budget.getPeriodMonth());
        budgetCreate.setPeriodYear(budget.getPeriodYear());
        budgetCreate.setPlannedIncome(budget.getPlannedIncome());
        budgetCreate.setDescription(budget.getDescription());

        Map<Long, BigDecimal> limits = new HashMap<>();
        budget.getLimits().forEach(l -> limits.put(l.getCategoryId(), l.getLimitAmount()));
        budgetCreate.setCategoryLimits(limits);

        var categories = categoryService.getUserCategories(userId, TransactionType.EXPENSE);

        model.addAttribute("budget", budgetCreate);
        model.addAttribute("budgetId", id);
        model.addAttribute("categories", categories);
        model.addAttribute("months", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        model.addAttribute("baseCurrency", user.getDefaultCurrency());
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute BudgetCreateDto budgetCreate,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();

            if (budgetCreate.getCategoryLimits() != null) {
                budgetCreate.getCategoryLimits().entrySet().removeIf(
                        entry -> entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) <= 0
                );
            }

            budgetService.updateBudget(userId, id, budgetCreate);
            redirectAttributes.addFlashAttribute("success", "Бюджет успешно обновлен");

        } catch (Exception e) {
            log.error("Error updating budget", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления бюджета: " + e.getMessage());
        }

        return "redirect:/budgets/active";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            budgetService.deleteBudget(userId, id);
            redirectAttributes.addFlashAttribute("success", "Бюджет удален");

        } catch (Exception e) {
            log.error("Error deleting budget", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления бюджета: " + e.getMessage());
        }

        return "redirect:/budgets";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            budgetService.completeBudget(userId, id);
            redirectAttributes.addFlashAttribute("success", "Бюджет завершен");

        } catch (Exception e) {
            log.error("Error completing budget", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/budgets";
    }

    @PostMapping("/{id}/refresh")
    public String refresh(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            budgetService.refreshBudgetStats(userId, id);
            redirectAttributes.addFlashAttribute("success", "Статистика бюджета обновлена");

        } catch (Exception e) {
            log.error("Error refreshing budget", e);
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/budgets/active";
    }
}