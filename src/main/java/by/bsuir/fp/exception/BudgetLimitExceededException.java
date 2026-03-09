package by.bsuir.fp.exception;

public class BudgetLimitExceededException extends RuntimeException {
    public BudgetLimitExceededException(String message) {
        super(message);
    }
}