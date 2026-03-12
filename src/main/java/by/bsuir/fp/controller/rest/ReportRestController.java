package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.TransactionFilterDto;
import by.bsuir.fp.service.ReportService;
import by.bsuir.fp.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportRestController {

    private final SecurityService securityService;
    private final ReportService reportService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdfReport(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        Long userId = securityService.getCurrentUserId();

        TransactionFilterDto filter = new TransactionFilterDto();
        filter.setFromDate(fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1));
        filter.setToDate(toDate != null ? toDate : LocalDate.now());
        filter.setPage(0);
        filter.setSize(1000); // максимум 1000 транзакций в отчете

        byte[] pdfBytes = reportService.generatePdfReport(userId, filter);

        String filename = "report_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> generateExcelReport(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        Long userId = securityService.getCurrentUserId();

        TransactionFilterDto filter = new TransactionFilterDto();
        filter.setFromDate(fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1));
        filter.setToDate(toDate != null ? toDate : LocalDate.now());
        filter.setPage(0);
        filter.setSize(1000);

        byte[] excelBytes = reportService.generateExcelReport(userId, filter);

        String filename = "report_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}