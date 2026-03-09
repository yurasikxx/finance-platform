package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.CurrencyRateDto;
import by.bsuir.fp.controller.dto.NbrbRateDto;
import by.bsuir.fp.model.CurrencyRate;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRateRepository currencyRateRepository;
    private final RestClient nbrbRestClient;

    private static final Map<CurrencyCode, Integer> NBRB_CURRENCY_IDS = Map.of(
            CurrencyCode.USD, 145,
            CurrencyCode.EUR, 19,
            CurrencyCode.RUB, 17
    );

    @Transactional(readOnly = true)
    public List<CurrencyRateDto> getCurrentRates() {
        LocalDate today = LocalDate.now();

        return Arrays.stream(CurrencyCode.values())
                .filter(currency -> currency != CurrencyCode.BYN)
                .map(currency -> getRateForDate(currency, today))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CurrencyRateDto getRateForDate(CurrencyCode currency, LocalDate date) {
        if (currency == CurrencyCode.BYN) {
            return CurrencyRateDto.builder()
                    .currency(CurrencyCode.BYN)
                    .rateToByn(BigDecimal.ONE)
                    .rateDate(date)
                    .build();
        }

        return currencyRateRepository.findByCurrencyAndRateDate(currency, date)
                .map(this::mapToDto)
                .orElseGet(() -> fetchAndSaveRate(currency, date));
    }

    @Transactional
    public CurrencyRateDto fetchAndSaveRate(CurrencyCode currency, LocalDate date) {
        try {
            Integer currencyId = NBRB_CURRENCY_IDS.get(currency);
            if (currencyId == null) {
                log.warn("No NBRB currency ID for: {}", currency);
                return null;
            }

            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

            NbrbRateDto rateDto = nbrbRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/exrates/rates/{currencyId}")
                            .queryParam("ondate", dateStr)
                            .queryParam("periodicity", 0)
                            .build(currencyId))
                    .retrieve()
                    .body(NbrbRateDto.class);

            if (rateDto != null && rateDto.getCurOfficialRate() != null) {
                BigDecimal rate = rateDto.getCurOfficialRate();

                if (rateDto.getCurScale() != null && rateDto.getCurScale() > 1) {
                    rate = rate.divide(BigDecimal.valueOf(rateDto.getCurScale()), 4, RoundingMode.HALF_UP);
                }

                CurrencyRate currencyRate = CurrencyRate.builder()
                        .currency(currency)
                        .rateToByn(rate)
                        .rateDate(date)
                        .build();

                CurrencyRate saved = currencyRateRepository.save(currencyRate);
                log.info("Saved rate for {} on {}: {}", currency, date, rate);

                return mapToDto(saved);
            }
        } catch (Exception e) {
            log.error("Failed to fetch rate for {} on {}: {}", currency, date, e.getMessage());
        }

        return null;
    }

    @Scheduled(cron = "0 0 12 * * *") // каждый день в 12:00
    @Transactional
    public void updateDailyRates() {
        log.info("Updating daily currency rates");
        LocalDate today = LocalDate.now();

        for (CurrencyCode currency : NBRB_CURRENCY_IDS.keySet()) {
            fetchAndSaveRate(currency, today);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, CurrencyCode from, CurrencyCode to, LocalDate date) {
        if (from == to) {
            return amount;
        }

        CurrencyRateDto fromRate = getRateForDate(from, date);
        CurrencyRateDto toRate = getRateForDate(to, date);

        if (fromRate == null || toRate == null) {
            throw new RuntimeException("Unable to get exchange rates for conversion");
        }

        BigDecimal amountInByn = amount.multiply(fromRate.getRateToByn());
        return amountInByn.divide(toRate.getRateToByn(), 2, RoundingMode.HALF_UP);
    }

    private CurrencyRateDto mapToDto(CurrencyRate rate) {
        return CurrencyRateDto.builder()
                .currency(rate.getCurrency())
                .rateToByn(rate.getRateToByn())
                .rateDate(rate.getRateDate())
                .build();
    }
}