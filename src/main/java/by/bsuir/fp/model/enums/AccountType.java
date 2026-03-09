package by.bsuir.fp.model.enums;

public enum AccountType {
    CASH("Наличные"),
    BANK_CARD("Банковская карта"),
    ELECTRONIC_WALLET("Электронный кошелек"),
    DEPOSIT("Депозит"),
    INVESTMENT("Инвестиционный счет");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}