package com.restaurantpnl.auth;

import java.util.List;

public record AppUser(
        String id,
        String email,
        String name,
        String passwordHash,
        List<String> restaurantIds
) {
}
