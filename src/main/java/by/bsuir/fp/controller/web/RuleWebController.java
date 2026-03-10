package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.RuleDto;
import by.bsuir.fp.model.enums.RuleOperator;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.CategorizationRuleService;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/rules")
@RequiredArgsConstructor
public class RuleWebController {

    private final CategorizationRuleService ruleService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<RuleDto> rules = ruleService.getUserRules(userId);
        List<RuleDto> activeRules = ruleService.getActiveRules(userId);

        model.addAttribute("rules", rules);
        model.addAttribute("activeRules", activeRules);
        return "rules/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();

        model.addAttribute("rule", new RuleDto());
        model.addAttribute("fields", List.of("description", "amount"));
        model.addAttribute("operators", RuleOperator.values());
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("isEdit", false);

        return "rules/form";
    }

    @PostMapping
    public String create(@ModelAttribute RuleDto ruleDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleService.createRule(userId, ruleDto);
        return "redirect:/rules";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long userId = SecurityUtils.getCurrentUserId();

        RuleDto rule = ruleService.getRuleById(userId, id);

        model.addAttribute("rule", rule);
        model.addAttribute("fields", List.of("description", "amount"));
        model.addAttribute("operators", RuleOperator.values());
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("isEdit", true);

        return "rules/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute RuleDto ruleDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleService.updateRule(userId, id, ruleDto);
        return "redirect:/rules";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleService.deleteRule(userId, id);
        return "redirect:/rules";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam Boolean active) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleService.toggleRuleActive(userId, id, active);
        return "redirect:/rules";
    }
}