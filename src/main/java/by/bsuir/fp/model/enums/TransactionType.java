package by.bsuir.fp.model.enums;

public enum TransactionType {
    INCOME("Доход"),
    EXPENSE("Расход"),
    TRANSFER("Перевод между счетами");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}