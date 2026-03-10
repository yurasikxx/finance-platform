package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.AnalyticsDashboardDto;
import by.bsuir.fp.controller.dto.AnalyticsRequestDto;
import by.bsuir.fp.controller.dto.CategoryBreakdownDto;
import by.bsuir.fp.model.enums.TransactionType;
import by.bsuir.fp.service.AnalyticsService;
import by.bsuir.fp.util.SecurityUtils;
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

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDto> getDashboard(AnalyticsRequestDto request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getDashboard(userId, request));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<List<CategoryBreakdownDto>> getCategoryBreakdown(
            @RequestParam TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(userId, type, fromDate, toDate));
    }
}