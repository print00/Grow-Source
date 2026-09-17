package com.restaurantpnl.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Transaction(
        String id,
        String restaurantId,
        LocalDate date,
        String vendor,
        String description,
        BigDecimal amount,
        Category category,
        Confidence confidence,
        Direction direction
) {
    public BigDecimal absoluteAmount() {
        return amount.abs();
    }
}
