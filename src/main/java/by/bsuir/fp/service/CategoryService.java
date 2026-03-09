package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.CategoryDto;
import by.bsuir.fp.exception.CategoryNotFoundException;
import by.bsuir.fp.exception.UserNotFoundException;
import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.repository.CategoryRepository;
import by.bsuir.fp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CategoryDto createCategory(Long userId, CategoryDto categoryDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        Category category = Category.builder()
                .user(user)
                .name(categoryDto.getName())
                .type(categoryDto.getType())
                .color(categoryDto.getColor())
                .icon(categoryDto.getIcon())
                .description(categoryDto.getDescription())
                .isDefault(false)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToDto(savedCategory);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

        if (!category.getUser().getId().equals(userId) && !category.getIsDefault()) {
            throw new SecurityException("Нет доступа к этой категории");
        }

        return mapToDto(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getUserCategories(Long userId, TransactionType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Category> categories;
        if (type != null) {
            categories = categoryRepository.findByUserAndType(user, type);
        } else {
            categories = categoryRepository.findByUser(user);
        }

        return categories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllAvailableCategories(Long userId, TransactionType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Category> userCategories = type != null
                ? categoryRepository.findByUserAndType(user, type)
                : categoryRepository.findByUser(user);

        List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();

        defaultCategories.stream()
                .filter(dc -> !userCategories.stream()
                        .map(Category::getName)
                        .collect(Collectors.toSet())
                        .contains(dc.getName()))
                .forEach(userCategories::add);

        return userCategories.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto updateCategory(Long userId, Long categoryId, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

        if (!category.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой категории");
        }

        category.setName(categoryDto.getName());
        category.setColor(categoryDto.getColor());
        category.setIcon(categoryDto.getIcon());
        category.setDescription(categoryDto.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException("Категория не найдена"));

        if (!category.getUser().getId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой категории");
        }

        if (!category.getTransactions().isEmpty()) {
            throw new IllegalStateException("Нельзя удалить категорию с существующими транзакциями");
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public void createDefaultCategoriesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        List<Category> defaultCategories = categoryRepository.findByIsDefaultTrue();

        for (Category defaultCat : defaultCategories) {
            if (!categoryRepository.findByUserAndName(user, defaultCat.getName()).isPresent()) {
                Category newCategory = Category.builder()
                        .user(user)
                        .name(defaultCat.getName())
                        .type(defaultCat.getType())
                        .color(defaultCat.getColor())
                        .icon(defaultCat.getIcon())
                        .description(defaultCat.getDescription())
                        .isDefault(false)
                        .build();
                categoryRepository.save(newCategory);
            }
        }
    }

    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .color(category.getColor())
                .icon(category.getIcon())
                .description(category.getDescription())
                .isDefault(category.getIsDefault())
                .build();
    }
}