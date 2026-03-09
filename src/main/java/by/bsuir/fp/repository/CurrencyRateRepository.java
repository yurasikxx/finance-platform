package by.bsuir.fp.repository;

import by.bsuir.fp.model.CurrencyRate;
import by.bsuir.fp.model.enums.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    // Последний курс для валюты
    Optional<CurrencyRate> findTopByCurrencyOrderByRateDateDesc(CurrencyCode currency);

    // Курс на конкретную дату
    Optional<CurrencyRate> findByCurrencyAndRateDate(CurrencyCode currency, LocalDate rateDate);
}