package by.bsuir.fp.repository;

import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    long countByUser(User user);

    List<Transaction> findByUserAndIsCategorizedFalse(User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user = :user AND t.category = :category " +
            "AND t.type = 'EXPENSE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenseByCategoryAndPeriod(
            @Param("user") User user,
            @Param("category") Category category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = 'INCOME' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalIncomeByPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = 'EXPENSE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalExpenseByPeriod(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT t.transactionDate, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = 'EXPENSE' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.transactionDate ORDER BY t.transactionDate")
    List<Object[]> getDailyExpenses(@Param("user") User user,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    @Query("SELECT t.transactionDate, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = 'INCOME' " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.transactionDate ORDER BY t.transactionDate")
    List<Object[]> getDailyIncomes(@Param("user") User user,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND t.category IS NOT NULL " +
            "GROUP BY t.category")
    List<Object[]> findByUserAndTypeAndTransactionDateBetweenGroupByCategory(
            @Param("user") User user,
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}