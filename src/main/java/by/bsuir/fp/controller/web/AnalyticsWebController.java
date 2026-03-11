package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.AnalyticsDashboardDto;
import by.bsuir.fp.controller.dto.AnalyticsRequestDto;
import by.bsuir.fp.controller.dto.CategoryBreakdownDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.service.TransactionService;
import by.bsuir.fp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsWebController {

    private final AnalyticsService analyticsService;
    private final TransactionService transactionService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Model model) {

        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate start = fromDate != null ? fromDate : YearMonth.now().atDay(1);
        LocalDate end = toDate != null ? toDate : LocalDate.now();

        AnalyticsRequestDto request = new AnalyticsRequestDto();
        request.setFromDate(start);
        request.setToDate(end);

        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(userId, request);

        List<String> expenseLabels = dashboard.getExpenseBreakdown().stream()
                .map(CategoryBreakdownDto::getCategoryName).toList();
        List<BigDecimal> expenseData = dashboard.getExpenseBreakdown().stream()
                .map(CategoryBreakdownDto::getAmount).toList();

        Map<LocalDate, BigDecimal> dailyExpenses = transactionService.getDailyExpenses(userId, start, end);
        Map<LocalDate, BigDecimal> dailyIncomes = transactionService.getDailyIncomes(userId, start, end);

        List<String> dateLabels = new ArrayList<>();
        List<BigDecimal> expenseValues = new ArrayList<>();
        List<BigDecimal> incomeValues = new ArrayList<>();

        LocalDate date = start;
        while (!date.isAfter(end)) {
            dateLabels.add(date.format(DateTimeFormatter.ofPattern("dd.MM")));
            expenseValues.add(dailyExpenses.getOrDefault(date, BigDecimal.ZERO));
            incomeValues.add(dailyIncomes.getOrDefault(date, BigDecimal.ZERO));
            date = date.plusDays(1);
        }

        var expenseBreakdown = analyticsService.getCategoryBreakdown(
                userId, TransactionType.EXPENSE, start, end);

        model.addAttribute("expenseBreakdown", expenseBreakdown);

        model.addAttribute("dashboard", dashboard);

        // Форматируем даты для input type="date" (нужен формат yyyy-MM-dd)
        model.addAttribute("fromDateInput", start.format(DateTimeFormatter.ISO_LOCAL_DATE));
        model.addAttribute("toDateInput", end.format(DateTimeFormatter.ISO_LOCAL_DATE));

        // Для отображения в таблицах
        model.addAttribute("fromDateDisplay", start.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        model.addAttribute("toDateDisplay", end.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        model.addAttribute("expenseLabels", expenseLabels);
        model.addAttribute("expenseData", expenseData);

        model.addAttribute("dateLabels", dateLabels);
        model.addAttribute("expenseValues", expenseValues);
        model.addAttribute("incomeValues", incomeValues);

        return "analytics/dashboard";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();

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

        var expenseBreakdown = analyticsService.getCategoryBreakdown(
                userId, TransactionType.EXPENSE, startOfMonth, now);

        model.addAttribute("monthStats", monthStats);
        model.addAttribute("yearStats", yearStats);
        model.addAttribute("expenseBreakdown", expenseBreakdown);

        return "analytics/reports";
    }
}