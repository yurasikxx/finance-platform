package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.CategoryDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryWebController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) TransactionType type, Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<CategoryDto> categories = categoryService.getUserCategories(userId, type);

        model.addAttribute("categories", categories);
        model.addAttribute("selectedType", type);
        model.addAttribute("types", TransactionType.values());
        return "categories/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("category", new CategoryDto());
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("isEdit", false);
        return "categories/form";
    }

    @PostMapping
    public String create(@ModelAttribute CategoryDto categoryDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryService.createCategory(userId, categoryDto);
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryDto category = categoryService.getCategoryById(userId, id);

        model.addAttribute("category", category);
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("isEdit", true);
        return "categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute CategoryDto categoryDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryService.updateCategory(userId, id, categoryDto);
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        categoryService.deleteCategory(userId, id);
        return "redirect:/categories";
    }
}