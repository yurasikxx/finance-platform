package by.bsuir.fp.controller.rest;

import by.bsuir.fp.controller.dto.CurrencyRateDto;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
public class CurrencyRestController {

    private final CurrencyService currencyService;

    @GetMapping("/rates")
    public ResponseEntity<List<CurrencyRateDto>> getCurrentRates() {
        return ResponseEntity.ok(currencyService.getCurrentRates());
    }

    @GetMapping("/rates/{currency}")
    public ResponseEntity<CurrencyRateDto> getRateForDate(
            @PathVariable CurrencyCode currency,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        CurrencyRateDto rate = currencyService.getRateForDate(currency, date);
        return rate != null ? ResponseEntity.ok(rate) : ResponseEntity.notFound().build();
    }

    @GetMapping("/convert")
    public ResponseEntity<BigDecimal> convert(
            @RequestParam BigDecimal amount,
            @RequestParam CurrencyCode from,
            @RequestParam CurrencyCode to,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ResponseEntity.ok(currencyService.convert(amount, from, to, date));
    }
}