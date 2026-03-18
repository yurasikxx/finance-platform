package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.CurrencyRateDto;
import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.CurrencyRate;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @InjectMocks
    private CurrencyService currencyService;

    private LocalDate testDate;
    private CurrencyRate usdRate;
    private CurrencyRate eurRate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2026, 3, 17);

        usdRate = new CurrencyRate();
        usdRate.setId(1L);
        usdRate.setCurrency(CurrencyCode.USD);
        usdRate.setRateToByn(new BigDecimal("3.45"));
        usdRate.setRateDate(testDate);

        eurRate = new CurrencyRate();
        eurRate.setId(2L);
        eurRate.setCurrency(CurrencyCode.EUR);
        eurRate.setRateToByn(new BigDecimal("3.75"));
        eurRate.setRateDate(testDate);

        CurrencyRate rubRate = new CurrencyRate();
        rubRate.setId(3L);
        rubRate.setCurrency(CurrencyCode.RUB);
        rubRate.setRateToByn(new BigDecimal("0.038"));
        rubRate.setRateDate(testDate);
    }

    @Test
    void getRateForDate_WhenRateExists_ShouldReturnFromDb() {
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.singletonList(usdRate));

        CurrencyRateDto result = currencyService.getRateForDate(CurrencyCode.USD, testDate);

        assertNotNull(result);
        assertEquals(CurrencyCode.USD, result.getCurrency());
        assertEquals(new BigDecimal("3.45"), result.getRateToByn());
    }

    @Test
    void getRateForDate_ForByn_ShouldReturnOne() {
        CurrencyRateDto result = currencyService.getRateForDate(CurrencyCode.BYN, testDate);

        assertNotNull(result);
        assertEquals(CurrencyCode.BYN, result.getCurrency());
        assertEquals(BigDecimal.ONE, result.getRateToByn());
    }

    @Test
    void getRateForDate_WhenRateNotFound_ShouldReturnNull() {
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.emptyList());
        when(currencyRateRepository.findTopByCurrencyOrderByRateDateDesc(CurrencyCode.USD))
                .thenReturn(Optional.empty());

        CurrencyRateDto result = currencyService.getRateForDate(CurrencyCode.USD, testDate);

        assertNull(result);
    }

    @Test
    void convert_UsdToByn_ShouldReturnCorrectAmount() {
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.singletonList(usdRate));

        BigDecimal result = currencyService.convert(
                new BigDecimal("100.00"),
                CurrencyCode.USD,
                CurrencyCode.BYN,
                testDate
        );

        assertEquals(new BigDecimal("345.00"), result);
    }

    @Test
    void convert_BynToUsd_ShouldReturnCorrectAmount() {
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.singletonList(usdRate));

        BigDecimal result = currencyService.convert(
                new BigDecimal("345.00"),
                CurrencyCode.BYN,
                CurrencyCode.USD,
                testDate
        );

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convert_UsdToEur_ShouldReturnCorrectAmount() {
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.singletonList(usdRate));
        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.EUR, testDate))
                .thenReturn(Collections.singletonList(eurRate));

        BigDecimal result = currencyService.convert(
                new BigDecimal("100.00"),
                CurrencyCode.USD,
                CurrencyCode.EUR,
                testDate
        );

        // 100 USD * 3.45 = 345 BYN / 3.75 = 92.00 EUR
        assertEquals(new BigDecimal("92.00"), result);
    }

    @Test
    void convert_SameCurrency_ShouldReturnSameAmount() {
        BigDecimal result = currencyService.convert(
                new BigDecimal("100.00"),
                CurrencyCode.USD,
                CurrencyCode.USD,
                testDate
        );

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convert_WithNullAmount_ShouldReturnZero() {
        BigDecimal result = currencyService.convert(
                null,
                CurrencyCode.USD,
                CurrencyCode.BYN,
                testDate
        );

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void convertTransaction_ShouldUseAccountCurrency() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setTransactionDate(testDate);

        Account account = new Account();
        account.setCurrency(CurrencyCode.USD);
        transaction.setAccount(account);

        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.singletonList(usdRate));

        BigDecimal result = currencyService.convertTransaction(transaction, CurrencyCode.BYN);

        assertEquals(new BigDecimal("172.50"), result);
    }

    @Test
    void convertTransaction_WithNullTransaction_ShouldReturnZero() {
        BigDecimal result = currencyService.convertTransaction(null, CurrencyCode.BYN);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getRateForDate_WhenNoRateForDateButHasLatest_ShouldReturnLatest() {
        CurrencyRate latestRate = new CurrencyRate();
        latestRate.setId(4L);
        latestRate.setCurrency(CurrencyCode.USD);
        latestRate.setRateToByn(new BigDecimal("3.50"));
        latestRate.setRateDate(testDate.minusDays(1));

        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(Collections.emptyList());
        when(currencyRateRepository.findTopByCurrencyOrderByRateDateDesc(CurrencyCode.USD))
                .thenReturn(Optional.of(latestRate));

        CurrencyRateDto result = currencyService.getRateForDate(CurrencyCode.USD, testDate);

        assertNotNull(result);
        assertEquals(new BigDecimal("3.50"), result.getRateToByn());
        assertEquals(testDate.minusDays(1), result.getRateDate());
    }

    @Test
    void getRateForDate_WhenMultipleRatesExist_ShouldReturnMostRecent() {
        CurrencyRate olderRate = new CurrencyRate();
        olderRate.setId(1L);
        olderRate.setCurrency(CurrencyCode.USD);
        olderRate.setRateToByn(new BigDecimal("3.40"));
        olderRate.setRateDate(testDate);

        CurrencyRate newerRate = new CurrencyRate();
        newerRate.setId(2L);
        newerRate.setCurrency(CurrencyCode.USD);
        newerRate.setRateToByn(new BigDecimal("3.45"));
        newerRate.setRateDate(testDate);

        List<CurrencyRate> rates = new ArrayList<>();
        rates.add(olderRate);
        rates.add(newerRate);

        when(currencyRateRepository.findByCurrencyAndRateDate(CurrencyCode.USD, testDate))
                .thenReturn(rates);

        CurrencyRateDto result = currencyService.getRateForDate(CurrencyCode.USD, testDate);

        assertNotNull(result);
        assertEquals(new BigDecimal("3.45"), result.getRateToByn());
    }
}