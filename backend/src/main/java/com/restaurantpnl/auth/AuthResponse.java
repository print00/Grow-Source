package com.restaurantpnl.auth;

import java.util.List;

public record AuthResponse(
        String token,
        UserSummary user,
        List<RestaurantAccess> restaurants
) {
    public record UserSummary(String id, String email, String name) {
    }

    public record RestaurantAccess(String id, String name, String role) {
    }
}
