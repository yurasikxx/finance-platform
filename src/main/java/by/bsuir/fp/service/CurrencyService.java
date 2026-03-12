package by.bsuir.fp.service;

import by.bsuir.fp.controller.dto.CurrencyRateDto;
import by.bsuir.fp.controller.dto.NbrbRateDto;
import by.bsuir.fp.model.CurrencyRate;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.enums.CurrencyCode;
import by.bsuir.fp.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    private static final Map<CurrencyCode, String> CURRENCY_CODES = Map.of(
            CurrencyCode.USD, "USD",
            CurrencyCode.EUR, "EUR",
            CurrencyCode.RUB, "RUB"
    );

    @Transactional(readOnly = true)
    public List<CurrencyRateDto> getCurrentRates() {
        LocalDate today = LocalDate.now();

        return Arrays.stream(CurrencyCode.values())
                .filter(currency -> currency != CurrencyCode.BYN)
                .map(currency -> getRateFromDatabase(currency, today))
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

        List<CurrencyRate> rates = currencyRateRepository.findByCurrencyAndRateDate(currency, date);
        if (!rates.isEmpty()) {
            rates.sort((a, b) -> b.getId().compareTo(a.getId()));
            return mapToDto(rates.getFirst());
        }

        CurrencyRate latest = currencyRateRepository.findTopByCurrencyOrderByRateDateDesc(currency)
                .orElse(null);
        if (latest != null) {
            log.warn("Using latest rate for {} from {} for date {}",
                    currency, latest.getRateDate(), date);
            return mapToDto(latest);
        }

        return null;
    }

    @Transactional(readOnly = true)
    public CurrencyRateDto getRateFromDatabase(CurrencyCode currency, LocalDate date) {
        List<CurrencyRate> rates = currencyRateRepository.findByCurrencyAndRateDate(currency, date);

        if (!rates.isEmpty()) {
            if (rates.size() > 1) {
                log.warn("Found {} rates for {} on {}, using the most recent",
                        rates.size(), currency, date);
                rates.sort((a, b) -> b.getId().compareTo(a.getId()));
            }
            return mapToDto(rates.getFirst());
        }

        CurrencyRate latest = currencyRateRepository.findTopByCurrencyOrderByRateDateDesc(currency)
                .orElse(null);
        if (latest != null) {
            log.warn("Using latest rate for {} from {} for date {}",
                    currency, latest.getRateDate(), date);
            return mapToDto(latest);
        }

        return null;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fetchAndSaveRate(CurrencyCode currency, LocalDate date) {
        try {
            String currencyCode = CURRENCY_CODES.get(currency);
            if (currencyCode == null) {
                log.warn("No currency code for: {}", currency);
                return;
            }

            String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String url = String.format("/exrates/rates/%s?parammode=2&ondate=%s",
                    currencyCode, dateStr);

            log.info("Fetching rate from NBRB API: {}", url);

            NbrbRateDto rateDto = nbrbRestClient.get()
                    .uri(url)
                    .retrieve()
                    .body(NbrbRateDto.class);

            if (rateDto != null && rateDto.getCurOfficialRate() != null) {
                BigDecimal rate = rateDto.getCurOfficialRate();

                if (rateDto.getCurScale() != null && rateDto.getCurScale() > 1) {
                    rate = rate.divide(BigDecimal.valueOf(rateDto.getCurScale()), 4, RoundingMode.HALF_UP);
                }

                List<CurrencyRate> oldRates = currencyRateRepository.findByCurrencyAndRateDate(currency, date);
                if (!oldRates.isEmpty()) {
                    log.info("Deleting {} old rate(s) for {} on {}", oldRates.size(), currency, date);
                    currencyRateRepository.deleteAll(oldRates);
                    currencyRateRepository.flush();
                }

                CurrencyRate currencyRate = CurrencyRate.builder()
                        .currency(currency)
                        .rateToByn(rate)
                        .rateDate(date)
                        .build();
                currencyRateRepository.save(currencyRate);

                log.info("Saved new rate for {} on {}: {}", currency, date, rate);

                CurrencyRateDto.builder()
                        .currency(currency)
                        .rateToByn(rate)
                        .rateDate(date)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch/save rate for {} on {}: {}", currency, date, e.getMessage());
        }

    }

    @Scheduled(cron = "0 0 12 * * *")
    @Transactional
    public void updateDailyRates() {
        log.info("Starting daily currency rates update");
        LocalDate today = LocalDate.now();

        for (CurrencyCode currency : CURRENCY_CODES.keySet()) {
            fetchAndSaveRate(currency, today);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, CurrencyCode from, CurrencyCode to, LocalDate date) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (from == to) return amount;

        log.debug("Converting {} {} to {} on {}", amount, from, to, date);

        CurrencyRateDto fromRate = getRateForDate(from, date);
        CurrencyRateDto toRate = getRateForDate(to, date);

        if (fromRate == null || toRate == null ||
                fromRate.getRateToByn() == null || toRate.getRateToByn() == null) {
            log.error("Missing rates for conversion {} -> {} on {}", from, to, date);
            return amount;
        }

        BigDecimal amountInByn = amount.multiply(fromRate.getRateToByn());
        BigDecimal result = amountInByn.divide(toRate.getRateToByn(), 2, RoundingMode.HALF_UP);

        log.debug("{} {} -> {} BYN -> {} {}", amount, from, amountInByn, result, to);

        return result;
    }

    @Transactional(readOnly = true)
    public BigDecimal convertTransaction(Transaction transaction, CurrencyCode targetCurrency) {
        if (transaction == null) return BigDecimal.ZERO;
        if (transaction.getAccount() == null) return transaction.getAmount();

        return convert(
                transaction.getAmount(),
                transaction.getAccount().getCurrency(),
                targetCurrency,
                transaction.getTransactionDate()
        );
    }

    private CurrencyRateDto mapToDto(CurrencyRate rate) {
        return CurrencyRateDto.builder()
                .currency(rate.getCurrency())
                .rateToByn(rate.getRateToByn())
                .rateDate(rate.getRateDate())
                .build();
    }
}