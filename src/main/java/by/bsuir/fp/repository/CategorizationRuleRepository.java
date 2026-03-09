package by.bsuir.fp.repository;

import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorizationRuleRepository extends JpaRepository<CategorizationRule, Long> {

    // Все активные правила пользователя, отсортированные по приоритету
    List<CategorizationRule> findByUserAndIsActiveTrueOrderByPriorityDesc(User user);

    // Все правила пользователя
    List<CategorizationRule> findByUser(User user);

    // Правила для конкретного поля
    List<CategorizationRule> findByUserAndField(User user, String field);
}