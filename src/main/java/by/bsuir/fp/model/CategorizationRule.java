package by.bsuir.fp.model;

import by.bsuir.fp.model.enums.RuleOperator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "categorization_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 50)
    private String field;  // поле для проверки: "description", "amount", "account"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleOperator operator;

    @Column(name = "value", nullable = false)
    private String value;  // значение для сравнения

    @Column(name = "priority")
    private Integer priority = 0;  // приоритет применения правил

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}