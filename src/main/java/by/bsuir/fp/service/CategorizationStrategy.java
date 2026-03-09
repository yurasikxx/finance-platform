package by.bsuir.fp.service;

import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.Transaction;

public interface CategorizationStrategy {
    boolean matches(Transaction transaction, CategorizationRule rule);
}