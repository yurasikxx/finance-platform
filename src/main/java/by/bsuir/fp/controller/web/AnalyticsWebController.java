package by.bsuir.fp.controller.web;

import by.bsuir.fp.controller.dto.AnalyticsDashboardDto;
import by.bsuir.fp.controller.dto.AnalyticsRequestDto;
import by.bsuir.fp.controller.dto.CategoryBreakdownDto;
import by.bsuir.fp.controller.dto.DailyTotalDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsWebController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public String dashboard(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model, HttpServletRequest request) {

        Long userId = SecurityUtils.getCurrentUserId();

        LocalDate start = fromDate != null ? LocalDate.parse(fromDate) : YearMonth.now().atDay(1);
        LocalDate end = toDate != null ? LocalDate.parse(toDate) : LocalDate.now();

        AnalyticsRequestDto requestDto = new AnalyticsRequestDto();
        requestDto.setFromDate(start);
        requestDto.setToDate(end);

        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(userId, requestDto);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("fromDate", start.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("toDate", end.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("currentUri", request.getRequestURI());

        model.addAttribute("expenseLabels",
                dashboard.getExpenseBreakdown().stream().map(CategoryBreakdownDto::getCategoryName).toList());
        model.addAttribute("expenseData",
                dashboard.getExpenseBreakdown().stream().map(CategoryBreakdownDto::getAmount).toList());

        model.addAttribute("dailyLabels",
                dashboard.getDailyTotals().stream().map(d -> d.getDate().toString()).toList());
        model.addAttribute("dailyIncome",
                dashboard.getDailyTotals().stream().map(DailyTotalDto::getIncome).toList());
        model.addAttribute("dailyExpense",
                dashboard.getDailyTotals().stream().map(DailyTotalDto::getExpense).toList());

        return "analytics/dashboard";
    }

    @GetMapping("/reports")
    public String reports(Model model, HttpServletRequest request) {
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
        model.addAttribute("currentUri", request.getRequestURI());

        return "analytics/reports";
    }
}