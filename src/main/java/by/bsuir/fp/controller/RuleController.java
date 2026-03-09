package by.bsuir.fp.controller;

import by.bsuir.fp.controller.dto.RuleDto;
import by.bsuir.fp.service.CategorizationRuleService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final CategorizationRuleService ruleService;

    @GetMapping
    public ResponseEntity<List<RuleDto>> getUserRules() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ruleService.getUserRules(userId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<RuleDto>> getActiveRules() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ruleService.getActiveRules(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleDto> getRuleById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ruleService.getRuleById(userId, id));
    }

    @PostMapping
    public ResponseEntity<RuleDto> createRule(@Valid @RequestBody RuleDto ruleDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ruleService.createRule(userId, ruleDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleDto> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody RuleDto ruleDto) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ruleService.updateRule(userId, id, ruleDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        ruleService.deleteRule(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleRule(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        Long userId = getCurrentUserId();
        ruleService.toggleRuleActive(userId, id, active);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/priority")
    public ResponseEntity<Void> updatePriority(
            @PathVariable Long id,
            @RequestParam Integer priority) {
        Long userId = getCurrentUserId();
        ruleService.updatePriority(userId, id, priority);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}