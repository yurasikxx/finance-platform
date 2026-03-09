package by.bsuir.fp.service;

import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmountCategorizationStrategy implements CategorizationStrategy {

    @Override
    public boolean matches(Transaction transaction, CategorizationRule rule) {
        if (transaction.getAmount() == null || rule.getValue() == null) {
            return false;
        }

        try {
            BigDecimal ruleAmount = new BigDecimal(rule.getValue());

            return switch (rule.getOperator()) {
                case GREATER_THAN -> transaction.getAmount().compareTo(ruleAmount) > 0;
                case LESS_THAN -> transaction.getAmount().compareTo(ruleAmount) < 0;
                case EQUALS -> transaction.getAmount().compareTo(ruleAmount) == 0;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return false;
        }
    }
}