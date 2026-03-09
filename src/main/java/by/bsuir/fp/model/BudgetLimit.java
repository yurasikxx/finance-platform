package by.bsuir.fp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "budget_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "spent_amount", precision = 10, scale = 2)
    private BigDecimal spentAmount = BigDecimal.ZERO;
}