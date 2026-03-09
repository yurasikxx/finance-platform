package by.bsuir.fp.model;

import by.bsuir.fp.model.enums.BudgetStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;  // 1-12

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "planned_income", precision = 10, scale = 2)
    private BigDecimal plannedIncome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetLimit> limits = new ArrayList<>();
}