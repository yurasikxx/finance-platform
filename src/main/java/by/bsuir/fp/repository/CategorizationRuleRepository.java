package by.bsuir.fp.repository;

import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorizationRuleRepository extends JpaRepository<CategorizationRule, Long> {

    List<CategorizationRule> findByUserAndIsActiveTrueOrderByPriorityDesc(User user);

    List<CategorizationRule> findByUser(User user);

    long countByUserAndIsActiveTrue(User user);
}