package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.UserRegistrationDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.service.UserService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileWebController {

    private final UserService userService;

    @GetMapping
    public String profile(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);

        model.addAttribute("user", user);
        model.addAttribute("currentUri", request.getRequestURI());

        return "profile/index";
    }

    @GetMapping("/edit")
    public String editForm(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
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
    public String update(@ModelAttribute UserRegistrationDto editForm, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            userService.updateUser(userId, editForm);
            redirectAttributes.addFlashAttribute("success", "Профиль обновлен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}