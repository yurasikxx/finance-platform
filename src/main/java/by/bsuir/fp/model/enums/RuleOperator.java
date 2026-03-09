package by.bsuir.fp.model.enums;

public enum RuleOperator {
    CONTAINS("содержит"),
    EQUALS("равно"),
    GREATER_THAN("больше"),
    LESS_THAN("меньше"),
    STARTS_WITH("начинается с"),
    ENDS_WITH("заканчивается на");

    private final String description;

    RuleOperator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}