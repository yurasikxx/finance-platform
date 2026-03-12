package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.RuleDto;
import by.bsuir.fp.model.enums.RuleOperator;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.CategorizationRuleService;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleWebController {

    private final CategorizationRuleService ruleService;
    private final CategoryService categoryService;
    private final SecurityService securityService;

    @GetMapping
    public String list(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();

        List<RuleDto> rules = ruleService.getUserRules(userId);
        List<RuleDto> activeRules = ruleService.getActiveRules(userId);

        model.addAttribute("rules", rules);
        model.addAttribute("activeRules", activeRules);
        model.addAttribute("currentUri", request.getRequestURI());

        return "rules/list";
    }

    @GetMapping("/add")
    public String addForm(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();

        model.addAttribute("rule", new RuleDto());
        model.addAttribute("fields", List.of("description", "amount"));
        model.addAttribute("operators", RuleOperator.values());
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("isEdit", false);
        model.addAttribute("currentUri", request.getRequestURI());

        return "rules/form";
    }

    @PostMapping
    public String create(@ModelAttribute RuleDto ruleDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            ruleService.createRule(userId, ruleDto);
            redirectAttributes.addFlashAttribute("success", "Правило создано");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/rules";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();

        RuleDto rule = ruleService.getRuleById(userId, id);

        model.addAttribute("rule", rule);
        model.addAttribute("fields", List.of("description", "amount"));
        model.addAttribute("operators", RuleOperator.values());
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("isEdit", true);
        model.addAttribute("currentUri", request.getRequestURI());

        return "rules/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute RuleDto ruleDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            ruleService.updateRule(userId, id, ruleDto);
            redirectAttributes.addFlashAttribute("success", "Правило изменено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/rules";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            ruleService.deleteRule(userId, id);
            redirectAttributes.addFlashAttribute("success", "Правило удалено");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/rules";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam Boolean active, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            ruleService.toggleRuleActive(userId, id, active);
            String status = active ? "активировано" : "деактивировано";
            redirectAttributes.addFlashAttribute("success", "Правило " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/rules";
    }
}