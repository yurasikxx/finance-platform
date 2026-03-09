package by.bsuir.fp.repository;

import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.User;
import by.bsuir.fp.model.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> byUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    public static Specification<Transaction> byAccountId(Long accountId) {
        return (root, query, cb) -> accountId == null ? null :
                cb.equal(root.get("account").get("id"), accountId);
    }

    public static Specification<Transaction> byCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null :
                cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Transaction> byType(TransactionType type) {
        return (root, query, cb) -> type == null ? null :
                cb.equal(root.get("type"), type);
    }

    public static Specification<Transaction> byDateBetween(LocalDate fromDate, LocalDate toDate) {
        return (root, query, cb) -> {
            if (fromDate == null && toDate == null) return null;
            if (fromDate != null && toDate != null) {
                return cb.between(root.get("transactionDate"), fromDate, toDate);
            } else if (fromDate != null) {
                return cb.greaterThanOrEqualTo(root.get("transactionDate"), fromDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("transactionDate"), toDate);
            }
        };
    }

    public static Specification<Transaction> bySearchText(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.trim().isEmpty()) return null;
            String pattern = "%" + searchText.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("category").get("name")), pattern)
            );
        };
    }
}