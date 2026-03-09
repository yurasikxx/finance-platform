package by.bsuir.fp.model.enums;

public enum CurrencyCode {
    BYN("Белорусский рубль"),
    USD("Доллар США"),
    EUR("Евро"),
    RUB("Российский рубль");

    private final String description;

    CurrencyCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}