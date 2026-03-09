package by.bsuir.fp.repository;

import by.bsuir.fp.model.Account;
import by.bsuir.fp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    List<Account> findByUserAndType(User user, String type);

    boolean existsByUserAndName(User user, String name);

    // Получение суммарного баланса пользователя по всем счетам
    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.user = :user")
    BigDecimal getTotalBalanceByUser(@Param("user") User user);
}