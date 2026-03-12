package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.AnalyticsDashboardDto;
import by.bsuir.fp.controller.dto.AnalyticsRequestDto;
import by.bsuir.fp.controller.dto.CategoryBreakdownDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.service.SecurityService;
import by.bsuir.fp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsRestController {

    private final SecurityService securityService;
    private final UserService userService;
    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Long userId = securityService.getCurrentUserId();

        AnalyticsRequestDto request = new AnalyticsRequestDto();
        request.setFromDate(fromDate);
        request.setToDate(toDate);

        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(userId, request);

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/breakdown")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @RequestParam TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        Long userId = securityService.getCurrentUserId();
        var user = userService.getUserById(userId);

        LocalDate start = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate end = toDate != null ? toDate : LocalDate.now();

        List<CategoryBreakdownDto> breakdown = analyticsService.getCategoryBreakdownInBaseCurrency(
                userId, type, start, end, user.getDefaultCurrency()
        );

        return ResponseEntity.ok(breakdown);
    }
}