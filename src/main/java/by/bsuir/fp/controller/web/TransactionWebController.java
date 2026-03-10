package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.service.CategoryService;
import by.bsuir.fp.service.TransactionService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionWebController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType type,
            Model model, HttpServletRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        TransactionFilterDto filter = new TransactionFilterDto();
        filter.setPage(page);
        filter.setSize(20);
        filter.setAccountId(accountId);
        filter.setCategoryId(categoryId);
        filter.setType(type);

        Page<TransactionDto> transactionsPage = transactionService.getTransactions(userId, filter);

        model.addAttribute("transactions", transactionsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", transactionsPage.getTotalPages());
        model.addAttribute("totalElements", transactionsPage.getTotalElements());

        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("selectedAccountId", accountId);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedType", type);
        model.addAttribute("currentUri", request.getRequestURI());

        return "transactions/list";
    }

    @GetMapping("/add")
    public String addForm(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        model.addAttribute("transaction", new TransactionDto());
        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("expenseCategories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("incomeCategories", categoryService.getUserCategories(userId, TransactionType.INCOME));
        model.addAttribute("isEdit", false);
        model.addAttribute("currentUri", request.getRequestURI());

        return "transactions/form";
    }

    @PostMapping
    public String create(@ModelAttribute TransactionDto transactionDto, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            transactionService.createTransaction(userId, transactionDto);
            redirectAttributes.addFlashAttribute("success", "Транзакция успешно добавлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка добавления транзакции: " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        TransactionDto transaction = transactionService.getTransactionById(userId, id);

        model.addAttribute("transaction", transaction);
        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("expenseCategories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("incomeCategories", categoryService.getUserCategories(userId, TransactionType.INCOME));
        model.addAttribute("isEdit", true);
        model.addAttribute("currentUri", request.getRequestURI());

        return "transactions/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute TransactionDto transactionDto,
                         RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            transactionService.updateTransaction(userId, id, transactionDto);
            redirectAttributes.addFlashAttribute("success", "Транзакция обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления: " + e.getMessage());
        }

        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            transactionService.deleteTransaction(userId, id);
            redirectAttributes.addFlashAttribute("success", "Транзакция удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления: " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    @GetMapping("/import")
    public String importForm(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        model.addAttribute("accounts", accountService.getUserAccounts(userId));
        model.addAttribute("currentUri", request.getRequestURI());

        return "transactions/import";
    }

    @GetMapping("/uncategorized")
    public String uncategorized(Model model, HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        List<TransactionDto> uncategorized = transactionService.getUncategorizedTransactions(userId);
        model.addAttribute("transactions", uncategorized);
        model.addAttribute("categories", categoryService.getUserCategories(userId, TransactionType.EXPENSE));
        model.addAttribute("currentUri", request.getRequestURI());

        return "transactions/uncategorized";
    }

    @PostMapping("/{id}/categorize")
    public String categorize(@PathVariable Long id, @RequestParam Long categoryId, RedirectAttributes redirectAttributes) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            transactionService.categorizeTransaction(userId, id, categoryId);
            redirectAttributes.addFlashAttribute("success", "Категория назначена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка: " + e.getMessage());
        }
        return "redirect:/transactions/uncategorized";
    }
}