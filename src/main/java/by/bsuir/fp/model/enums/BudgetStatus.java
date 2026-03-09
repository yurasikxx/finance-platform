package by.bsuir.fp.model.enums;

public enum BudgetStatus {
    DRAFT("Черновик"),
    ACTIVE("Активен"),
    COMPLETED("Завершен"),
    ARCHIVED("Архивирован");

    private final String description;

    BudgetStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}