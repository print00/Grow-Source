package com.restaurantpnl.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RegisterRequest(
            @Email @NotBlank String email,
            @Size(min = 8) String password,
            @NotBlank String ownerName,
            @NotBlank String restaurantName,
            @NotBlank String restaurantType
    ) {
    }
}
