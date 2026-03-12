package by.bsuir.fp.repository;

import by.bsuir.fp.model.BudgetLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {
}