package by.bsuir.fp.repository;

import by.bsuir.fp.model.Category;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    long countByUser(User user);

    List<Transaction> findByUserAndIsCategorizedFalse(User user);

    List<Transaction> findByUserAndTransactionDateBetween(User user, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserAndTypeAndTransactionDateBetween(User user, TransactionType type, LocalDate startDate,
                                                                 LocalDate endDate);

    List<Transaction> findByUserAndCategoryAndTransactionDateBetween(User user, Category category,
                                                                     LocalDate startDate, LocalDate endDate);
}