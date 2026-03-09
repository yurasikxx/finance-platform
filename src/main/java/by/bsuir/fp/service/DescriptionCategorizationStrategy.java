package by.bsuir.fp.service;

import by.bsuir.fp.model.CategorizationRule;
import by.bsuir.fp.model.Transaction;
import by.bsuir.fp.model.enums.RuleOperator;
import org.springframework.stereotype.Component;

@Component
public class DescriptionCategorizationStrategy implements CategorizationStrategy {

    @Override
    public boolean matches(Transaction transaction, CategorizationRule rule) {
        if (transaction.getDescription() == null || rule.getValue() == null) {
            return false;
        }

        String desc = transaction.getDescription().toLowerCase();
        String ruleValue = rule.getValue().toLowerCase();

        return switch (rule.getOperator()) {
            case CONTAINS -> desc.contains(ruleValue);
            case EQUALS -> desc.equals(ruleValue);
            case STARTS_WITH -> desc.startsWith(ruleValue);
            case ENDS_WITH -> desc.endsWith(ruleValue);
            default -> false;
        };
    }
}