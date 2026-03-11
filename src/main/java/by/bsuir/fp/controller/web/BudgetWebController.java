package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.BudgetService;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetWebController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Integer year, Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        if (year == null) {
            year = YearMonth.now().getYear();
        }

        List<BudgetDto> budgets = budgetService.getUserBudgets(userId, year);

        model.addAttribute("budgets", budgets);
        model.addAttribute("selectedYear", year);
        model.addAttribute("years", List.of(year - 1, year, year + 1));
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/list";
    }

    @GetMapping("/active")
    public String active(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        YearMonth current = YearMonth.now();

        BudgetDto activeBudget = budgetService.getActiveBudget(
                userId,
                current.getMonthValue(),
                current.getYear()
        );

        model.addAttribute("budget", activeBudget);
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/active";
    }

    @GetMapping("/create")
    public String createForm(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        YearMonth current = YearMonth.now();

        BudgetCreateDto budgetCreate = new BudgetCreateDto();
        budgetCreate.setPeriodMonth(current.getMonthValue());
        budgetCreate.setPeriodYear(current.getYear());

        var categories = categoryService.getUserCategories(userId, TransactionType.EXPENSE);
        Map<Long, String> categoryMap = new HashMap<>();
        categories.forEach(c -> categoryMap.put(c.getId(), c.getName()));

        model.addAttribute("budget", budgetCreate);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryMap", categoryMap);
        model.addAttribute("months", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        model.addAttribute("currentYear", current.getYear());
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/form";
    }

    @PostMapping
    public String create(@ModelAttribute BudgetCreateDto budgetCreate, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();

            if (budgetCreate.getCategoryLimits() != null) {
                budgetCreate.getCategoryLimits().entrySet().removeIf(
                        entry -> entry.getValue() == null || entry.getValue().compareTo(BigDecimal.ZERO) <= 0
                );
            }

            budgetService.createBudget(userId, budgetCreate);
            redirectAttributes.addFlashAttribute("success", "Бюджет создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/budgets/active";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        BudgetDto budget = budgetService.getBudgetById(userId, id).orElse(null);
        if (budget == null) {
            return "redirect:/budgets";
        }

        BudgetCreateDto budgetCreate = new BudgetCreateDto();
        budgetCreate.setName(budget.getName());
        budgetCreate.setPeriodMonth(budget.getPeriodMonth());
        budgetCreate.setPeriodYear(budget.getPeriodYear());
        budgetCreate.setPlannedIncome(budget.getPlannedIncome());

        Map<Long, BigDecimal> limits = new HashMap<>();
        budget.getLimits().forEach(l -> limits.put(l.getCategoryId(), l.getLimitAmount()));
        budgetCreate.setCategoryLimits(limits);

        var categories = categoryService.getUserCategories(userId, TransactionType.EXPENSE);

        model.addAttribute("budget", budgetCreate);
        model.addAttribute("budgetId", id);
        model.addAttribute("categories", categories);
        model.addAttribute("months", List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        model.addAttribute("currentUri", request.getRequestURI());

        return "budgets/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute BudgetCreateDto budgetCreate,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            budgetService.updateBudget(userId, id, budgetCreate);
            redirectAttributes.addFlashAttribute("success", "Бюджет изменён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/budgets/active";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            budgetService.deleteBudget(userId, id);
            redirectAttributes.addFlashAttribute("success", "Бюджет удалён");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/budgets";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            budgetService.completeBudget(userId, id);
            redirectAttributes.addFlashAttribute("success", "Бюджет завершен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/budgets";
    }
}