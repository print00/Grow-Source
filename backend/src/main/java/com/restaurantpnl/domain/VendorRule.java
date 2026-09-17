package com.restaurantpnl.domain;

public record VendorRule(
        String vendorContains,
        Category category,
        Confidence confidence
) {
    public boolean matches(String vendor) {
        return vendor.toLowerCase().contains(vendorContains.toLowerCase());
    }
}
