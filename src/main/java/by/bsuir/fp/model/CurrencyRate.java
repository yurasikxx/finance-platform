package by.bsuir.fp.model;

import by.bsuir.fp.model.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "currency_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyCode currency;

    @Column(name = "rate_to_byn", nullable = false, precision = 10, scale = 4)
    private BigDecimal rateToByn;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}