package by.bsuir.fp.model;

import by.bsuir.fp.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(length = 7)  // для HEX цвета, например #FF5733
    private String color;

    @Column(length = 50)
    private String icon;

    @Column(length = 500)
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Связи
    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<CategorizationRule> rules = new ArrayList<>();

    @OneToMany(mappedBy = "category")
    private List<BudgetLimit> budgetLimits = new ArrayList<>();
}