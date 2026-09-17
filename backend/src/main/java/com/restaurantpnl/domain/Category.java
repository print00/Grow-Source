package com.restaurantpnl.domain;

public enum Category {
    REVENUE("Revenue", CategoryGroup.REVENUE),
    FOOD_BEVERAGE("Food & beverage", CategoryGroup.COGS),
    LABOR("Labor", CategoryGroup.EXPENSE),
    RENT("Rent", CategoryGroup.EXPENSE),
    DELIVERY_FEES("Delivery fees", CategoryGroup.EXPENSE),
    UTILITIES("Utilities", CategoryGroup.EXPENSE),
    OTHER_OPERATING("Other operating", CategoryGroup.EXPENSE);

    private final String label;
    private final CategoryGroup group;

    Category(String label, CategoryGroup group) {
        this.label = label;
        this.group = group;
    }

    public String label() {
        return label;
    }

    public CategoryGroup group() {
        return group;
    }
}
