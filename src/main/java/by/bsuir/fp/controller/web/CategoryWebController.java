package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.CategoryDto;
import by.bsuir.fp.model.enums.TransactionType;
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
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryWebController {

    private final CategoryService categoryService;
    private final SecurityService securityService;

    @GetMapping
    public String list(@RequestParam(required = false) TransactionType type, Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        List<CategoryDto> categories = categoryService.getUserCategories(userId, type);

        model.addAttribute("categories", categories);
        model.addAttribute("selectedType", type);
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUri", request.getRequestURI());

        return "categories/list";
    }

    @GetMapping("/add")
    public String addForm(Model model, HttpServletRequest request) {
        model.addAttribute("category", new CategoryDto());
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("isEdit", false);
        model.addAttribute("currentUri", request.getRequestURI());

        return "categories/form";
    }

    @PostMapping
    public String create(@ModelAttribute CategoryDto categoryDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            categoryService.createCategory(userId, categoryDto);
            redirectAttributes.addFlashAttribute("success", "Категория создана");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        CategoryDto category = categoryService.getCategoryById(userId, id);

        model.addAttribute("category", category);
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("isEdit", true);
        model.addAttribute("currentUri", request.getRequestURI());

        return "categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute CategoryDto categoryDto,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            categoryService.updateCategory(userId, id, categoryDto);
            redirectAttributes.addFlashAttribute("success", "Категория обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = securityService.getCurrentUserId();
            categoryService.deleteCategory(userId, id);
            redirectAttributes.addFlashAttribute("success", "Категория удалена");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "Нельзя удалить категорию с транзакциями");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/categories";
    }
}