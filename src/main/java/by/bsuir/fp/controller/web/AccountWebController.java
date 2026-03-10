package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.AccountDto;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountWebController {

    private final AccountService accountService;

    @GetMapping
    public String list(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AccountDto> accounts = accountService.getUserAccounts(userId);
        BigDecimal totalBalance = accountService.getTotalBalance(userId);

        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance);
        return "accounts/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("account", new AccountDto());
        model.addAttribute("isEdit", false);
        return "accounts/form";
    }

    @PostMapping
    public String create(@ModelAttribute AccountDto accountDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.createAccount(userId, accountDto);
        return "redirect:/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        AccountDto account = accountService.getAccountById(userId, id);

        model.addAttribute("account", account);
        model.addAttribute("isEdit", true);
        return "accounts/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute AccountDto accountDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.updateAccount(userId, id, accountDto);
        return "redirect:/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        accountService.deleteAccount(userId, id);
        return "redirect:/accounts";
    }
}