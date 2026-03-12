package by.bsuir.fp.exception;

public class TransactionAccessDeniedException extends RuntimeException {
    public TransactionAccessDeniedException(String message) {
        super(message);
    }
}