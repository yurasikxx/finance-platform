package by.bsuir.fp.model;

import by.bsuir.fp.model.enums.AccountType;
import by.bsuir.fp.model.enums.CurrencyCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyCode currency;

    @Column(name = "initial_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal initialBalance;

    @Column(name = "current_balance", precision = 10, scale = 2, nullable = false)
    private BigDecimal currentBalance;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();
}