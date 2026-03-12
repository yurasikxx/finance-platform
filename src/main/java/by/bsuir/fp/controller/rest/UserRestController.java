package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.UserRegistrationDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategorizationRuleService ruleService;
    private final SecurityService securityService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(user);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Long userId = securityService.getCurrentUserId();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTransactions", transactionService.countUserTransactions(userId));
        stats.put("totalAccounts", accountService.countUserAccounts(userId));
        stats.put("activeRules", ruleService.countActiveRules(userId));

        return ResponseEntity.ok(stats);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserRegistrationDto updateDto) {

        UserResponseDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        UserResponseDto updatedUser = userService.updateUser(currentUser.getId(), updateDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        userService.deleteUser(currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}