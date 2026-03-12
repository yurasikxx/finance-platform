package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.AnalyticsDashboardDto;
import by.bsuir.fp.controller.dto.AnalyticsRequestDto;
import by.bsuir.fp.controller.dto.CategoryBreakdownDto;
import by.bsuir.fp.controller.dto.UserResponseDto;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.TransactionService;
import by.bsuir.fp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsWebController {

    private final SecurityService securityService;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final TransactionService transactionService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model,
            HttpServletRequest request) {

        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);
        CurrencyCode baseCurrency = user.getDefaultCurrency();

        LocalDate start = fromDate != null ? fromDate : YearMonth.now().atDay(1);
        LocalDate end = toDate != null ? toDate : LocalDate.now();

        AnalyticsRequestDto analyticsRequest = new AnalyticsRequestDto();
        analyticsRequest.setFromDate(start);
        analyticsRequest.setToDate(end);

        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(userId, analyticsRequest);

        Map<LocalDate, BigDecimal> dailyExpenses = transactionService.getDailyExpensesInBaseCurrency(
                userId, start, end, baseCurrency);
        Map<LocalDate, BigDecimal> dailyIncomes = transactionService.getDailyIncomesInBaseCurrency(
                userId, start, end, baseCurrency);

        List<String> dateLabels = new ArrayList<>();
        List<BigDecimal> expenseValues = new ArrayList<>();
        List<BigDecimal> incomeValues = new ArrayList<>();

        LocalDate current = start;
        while (!current.isAfter(end)) {
            dateLabels.add(current.format(DateTimeFormatter.ofPattern("dd.MM")));
            expenseValues.add(dailyExpenses.getOrDefault(current, BigDecimal.ZERO));
            incomeValues.add(dailyIncomes.getOrDefault(current, BigDecimal.ZERO));
            current = current.plusDays(1);
        }

        List<CategoryBreakdownDto> expenseBreakdown = analyticsService.getCategoryBreakdownInBaseCurrency(
                userId, TransactionType.EXPENSE, start, end, baseCurrency);

        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("fromDate", start.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("toDate", end.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("fromDateDisplay", start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        model.addAttribute("toDateDisplay", end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        model.addAttribute("expenseLabels", dashboard.getExpenseBreakdown().stream()
                .map(CategoryBreakdownDto::getCategoryName).toList());
        model.addAttribute("expenseData", dashboard.getExpenseBreakdown().stream()
                .map(CategoryBreakdownDto::getAmount).toList());

        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("expenseValues", expenseValues);
        model.addAttribute("incomeValues", incomeValues);

        model.addAttribute("expenseBreakdown", expenseBreakdown);
        model.addAttribute("currentUri", request.getRequestURI());

        return "analytics/dashboard";
    }

    @GetMapping("/reports")
    public String reports(Model model, HttpServletRequest request) {
        Long userId = securityService.getCurrentUserId();
        UserResponseDto user = userService.getUserById(userId);
        CurrencyCode baseCurrency = user.getDefaultCurrency();

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = YearMonth.now().atDay(1);
        LocalDate startOfYear = LocalDate.of(now.getYear(), 1, 1);

        AnalyticsRequestDto monthRequest = new AnalyticsRequestDto();
        monthRequest.setFromDate(startOfMonth);
        monthRequest.setToDate(now);
        AnalyticsDashboardDto monthStats = analyticsService.getDashboard(userId, monthRequest);

        AnalyticsRequestDto yearRequest = new AnalyticsRequestDto();
        yearRequest.setFromDate(startOfYear);
        yearRequest.setToDate(now);
        AnalyticsDashboardDto yearStats = analyticsService.getDashboard(userId, yearRequest);

        List<CategoryBreakdownDto> expenseBreakdown = analyticsService.getCategoryBreakdownInBaseCurrency(
                userId, TransactionType.EXPENSE, startOfMonth, now, baseCurrency);

        model.addAttribute("baseCurrency", baseCurrency);
        model.addAttribute("monthStats", monthStats);
        model.addAttribute("yearStats", yearStats);
        model.addAttribute("expenseBreakdown", expenseBreakdown);
        model.addAttribute("currentUri", request.getRequestURI());

        return "analytics/reports";
    }
}