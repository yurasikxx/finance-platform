package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.TransactionDto;
import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AccountService;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.service.BudgetService;
import by.bsuir.fp.service.TransactionService;
import by.bsuir.fp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardWebController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = YearMonth.now().atDay(1);

        BigDecimal totalBalance = accountService.getTotalBalance(userId);
        Map<String, BigDecimal> stats = transactionService.getDailyStats(userId, startOfMonth, now);
        BigDecimal monthlyIncome = stats.getOrDefault("income", BigDecimal.ZERO);
        BigDecimal monthlyExpense = stats.getOrDefault("expense", BigDecimal.ZERO);

        TransactionFilterDto filter = new TransactionFilterDto();
        filter.setPage(0);
        filter.setSize(10);
        Page<TransactionDto> recentTransactionsPage = transactionService.getTransactions(userId, filter);
        List<TransactionDto> recentTransactions = recentTransactionsPage.getContent();

        var activeBudget = budgetService.getActiveBudget(userId, now.getMonthValue(), now.getYear());
        var expenseBreakdown = analyticsService.getCategoryBreakdown(
                userId, TransactionType.EXPENSE, startOfMonth, now
        );

        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("monthlyExpense", monthlyExpense);
        model.addAttribute("netBalance", monthlyIncome.subtract(monthlyExpense));
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("activeBudget", activeBudget);
        model.addAttribute("expenseBreakdown", expenseBreakdown);

        return "dashboard";
    }
}