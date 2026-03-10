package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.AccountDto;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountWebController {

    private final AccountService accountService;

    @GetMapping
    public String list(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AccountDto> accounts = accountService.getUserAccounts(userId);
        BigDecimal totalBalance = accountService.getTotalBalance(userId);

        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("currentUri", request.getRequestURI());
        return "accounts/list";
    }

    @GetMapping("/add")
    public String addForm(Model model, HttpServletRequest request) {
        model.addAttribute("account", new AccountDto());
        model.addAttribute("isEdit", false);
        model.addAttribute("currentUri", request.getRequestURI());
        return "accounts/form";
    }

    @PostMapping
    public String create(@ModelAttribute AccountDto accountDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            accountService.createAccount(userId, accountDto);
            redirectAttributes.addFlashAttribute("success", "Счет успешно создан");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания счета: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            AccountDto account = accountService.getAccountById(userId, id);

            model.addAttribute("account", account);
            model.addAttribute("isEdit", true);
            model.addAttribute("currentUri", request.getRequestURI());
            return "accounts/form";
        } catch (Exception e) {
            return "redirect:/accounts";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute AccountDto accountDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            accountService.updateAccount(userId, id, accountDto);
            redirectAttributes.addFlashAttribute("success", "Счет успешно обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления счета: " + e.getMessage());
        }
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            accountService.deleteAccount(userId, id);
            redirectAttributes.addFlashAttribute("success", "Счет успешно удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления счета: " + e.getMessage());
        }
        return "redirect:/accounts";
    }
}