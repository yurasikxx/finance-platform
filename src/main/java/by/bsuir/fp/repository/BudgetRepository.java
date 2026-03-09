package by.bsuir.fp.repository;

import by.bsuir.fp.model.Budget;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.BudgetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUser(User user);

    Optional<Budget> findByUserAndPeriodMonthAndPeriodYearAndStatus(
            User user, Integer month, Integer year, BudgetStatus status);

    List<Budget> findByUserAndPeriodYear(User user, Integer year);

    // Бюджеты, активные на указанную дату
    @Query("SELECT b FROM Budget b WHERE b.user = :user " +
            "AND :date BETWEEN b.startDate AND b.endDate")
    List<Budget> findActiveBudgetsOnDate(
            @Param("user") User user,
            @Param("date") LocalDate date);
}