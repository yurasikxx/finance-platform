package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.UserRegistrationDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileWebController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategorizationRuleService ruleService;
    private final SecurityService securityService;

    @GetMapping
    public String profile(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);

        model.addAttribute("totalTransactions", transactionService.countUserTransactions(userId));
        model.addAttribute("totalAccounts", accountService.countUserAccounts(userId));
        model.addAttribute("activeRules", ruleService.countActiveRules(userId));
        model.addAttribute("user", user);
        model.addAttribute("currentUri", request.getRequestURI());

        return "profile/index";
    }

    @GetMapping("/edit")
    public String editForm(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);

        UserRegistrationDto editForm = new UserRegistrationDto();
        editForm.setUsername(user.getUsername());
        editForm.setEmail(user.getEmail());
        editForm.setDefaultCurrency(user.getDefaultCurrency());

        model.addAttribute("user", editForm);
        model.addAttribute("currencies", CurrencyCode.values());
        model.addAttribute("currentUri", request.getRequestURI());

        return "profile/edit";
    }

    @PostMapping("/edit")
    public String update(
            @ModelAttribute UserRegistrationDto editForm,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            @RequestParam(required = false) String confirmPassword,
            RedirectAttributes redirectAttributes) {

        try {
            Long userId = securityService.getCurrentUserId();

            if (newPassword != null && !newPassword.isEmpty()) {
                if (currentPassword == null || currentPassword.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "Введите текущий пароль");
                    return "redirect:/profile/edit";
                }

                if (!userService.checkPassword(userId, currentPassword)) {
                    redirectAttributes.addFlashAttribute("error", "Неверный текущий пароль");
                    return "redirect:/profile/edit";
                }

                if (!newPassword.equals(confirmPassword)) {
                    redirectAttributes.addFlashAttribute("error", "Новый пароль и подтверждение не совпадают");
                    return "redirect:/profile/edit";
                }

                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "Пароль должен содержать минимум 6 символов");
                    return "redirect:/profile/edit";
                }

                editForm.setPassword(newPassword);
            }

            userService.updateUser(userId, editForm);
            redirectAttributes.addFlashAttribute("success", "Профиль успешно обновлен");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @PostMapping("/delete")
    public String deleteAccount(RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            Long userId = securityService.getCurrentUserId();

            userService.deleteUser(userId);

            session.invalidate();

            redirectAttributes.addFlashAttribute("success", "Аккаунт успешно удален");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления аккаунта: " + e.getMessage());
            return "redirect:/profile";
        }

        return "redirect:/login";
    }
}