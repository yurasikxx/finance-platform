package by.bsuir.fp.repository;

import by.bsuir.fp.model.Budget;
import by.bsuir.fp.model.BudgetLimit;
import by.bsuir.fp.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {

    List<BudgetLimit> findByBudget(Budget budget);

    Optional<BudgetLimit> findByBudgetAndCategory(Budget budget, Category category);

    // Обновление потраченной суммы
    @Modifying
    @Query("UPDATE BudgetLimit bl SET bl.spentAmount = :spentAmount " +
            "WHERE bl.budget = :budget AND bl.category = :category")
    void updateSpentAmount(
            @Param("budget") Budget budget,
            @Param("category") Category category,
            @Param("spentAmount") BigDecimal spentAmount);
}