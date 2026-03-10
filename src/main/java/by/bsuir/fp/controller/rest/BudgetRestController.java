package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.BudgetCreateDto;
import by.bsuir.fp.controller.dto.BudgetDto;
import by.bsuir.fp.service.BudgetService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetRestController {

    private final BudgetService budgetService;

    @GetMapping("/active")
    public ResponseEntity<BudgetDto> getActiveBudget(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        BudgetDto budget = budgetService.getActiveBudget(userId, month, year);
        return budget != null ? ResponseEntity.ok(budget) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<BudgetDto>> getUserBudgets(
            @RequestParam(required = false) Integer year) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.getUserBudgets(userId, year));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BudgetDto> getBudgetById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return budgetService.getBudgetById(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BudgetDto> createBudget(@Valid @RequestBody BudgetCreateDto createDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.createBudget(userId, createDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetDto> updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetCreateDto updateDto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.updateBudget(userId, id, updateDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.deleteBudget(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        budgetService.completeBudget(userId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<BudgetDto> refreshBudget(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(budgetService.refreshBudgetStats(userId, id));
    }
}