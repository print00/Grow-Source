package com.restaurantpnl.api;

import com.restaurantpnl.auth.AppUser;
import com.restaurantpnl.auth.AuthRepository;
import com.restaurantpnl.auth.AuthRequests.LoginRequest;
import com.restaurantpnl.auth.AuthRequests.RegisterRequest;
import com.restaurantpnl.auth.AuthResponse;
import com.restaurantpnl.auth.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(AuthRepository authRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = authRepository.findByEmail(request.email())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.passwordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return responseFor(user);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        authRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        });

        AppUser user = authRepository.createUser(
                request.email(),
                request.ownerName(),
                passwordEncoder.encode(request.password()),
                request.restaurantName(),
                request.restaurantType()
        );
        return responseFor(user);
    }

    @GetMapping("/me")
    public AuthResponse me(@AuthenticationPrincipal AppUser user) {
        return responseFor(user);
    }

    private AuthResponse responseFor(AppUser user) {
        return new AuthResponse(
                jwtService.issue(user),
                new AuthResponse.UserSummary(user.id(), user.email(), user.name()),
                authRepository.restaurantAccess(user)
        );
    }
}
