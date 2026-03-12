package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.*;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardWebController {

    private final SecurityService securityService;
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AnalyticsService analyticsService;
    private final BudgetService budgetService;
    private final CurrencyService currencyService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);
        CurrencyCode baseCurrency = user.getDefaultCurrency();

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = YearMonth.now().atDay(1);

        BigDecimal totalBalance = accountService.getTotalBalanceInBaseCurrency(userId, baseCurrency);

        Map<String, BigDecimal> monthlyStats = transactionService.getMonthlyStatsInBaseCurrency(userId, baseCurrency);
        BigDecimal monthlyIncome = monthlyStats.get("income");
        BigDecimal monthlyExpense = monthlyStats.get("expense");
        BigDecimal netBalance = monthlyIncome.subtract(monthlyExpense);

        TransactionFilterDto filter = new TransactionFilterDto();
        filter.setPage(0);
        filter.setSize(10);
        Page<TransactionDto> recentTransactionsPage = transactionService.getTransactions(userId, filter);
        List<TransactionDto> recentTransactions = recentTransactionsPage.getContent();

        BudgetDto activeBudget = budgetService.getActiveBudget(userId, now.getMonthValue(), now.getYear());

        List<CategoryBreakdownDto> expenseBreakdown = analyticsService.getCategoryBreakdownInBaseCurrency(
                userId, TransactionType.EXPENSE, startOfMonth, now, baseCurrency
        );

        Map<LocalDate, BigDecimal> dailyExpenses = transactionService.getDailyExpensesInBaseCurrency(
                userId, startOfMonth, now, baseCurrency
        );

        List<String> dateLabels = dailyExpenses.keySet().stream()
                .map(date -> date.format(DateTimeFormatter.ofPattern("dd.MM")))
                .toList();
        List<BigDecimal> expenseValues = dailyExpenses.values().stream().toList();

        List<CurrencyRateDto> currencyRates = currencyService.getCurrentRates()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("monthlyExpense", monthlyExpense);
        model.addAttribute("netBalance", netBalance);
        model.addAttribute("recentTransactions", recentTransactions);
        model.addAttribute("activeBudget", activeBudget);
        model.addAttribute("expenseBreakdown", expenseBreakdown);
        model.addAttribute("expenseLabels", expenseBreakdown.stream().map(CategoryBreakdownDto::getCategoryName).toList());
        model.addAttribute("expenseData", expenseBreakdown.stream().map(CategoryBreakdownDto::getAmount).toList());
        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("expenseValues", expenseValues);
        model.addAttribute("currencyRates", currencyRates);
        model.addAttribute("currentDate", now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        model.addAttribute("currentUri", request.getRequestURI());

        return "dashboard";
    }
}