package by.bsuir.fp.controller;

import by.bsuir.fp.controller.dto.CategoryDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getUserCategories(
            @RequestParam(required = false) TransactionType type) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(categoryService.getUserCategories(userId, type));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryDto>> getAllAvailableCategories(
            @RequestParam(required = false) TransactionType type) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(categoryService.getAllAvailableCategories(userId, type));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(categoryService.getCategoryById(userId, id));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(categoryService.createCategory(userId, categoryDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto categoryDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(categoryService.updateCategory(userId, id, categoryDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        categoryService.deleteCategory(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}