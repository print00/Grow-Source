package com.restaurantpnl.auth;

import com.restaurantpnl.domain.Restaurant;
import com.restaurantpnl.service.DemoRestaurantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuthRepository {
    private final Map<String, AppUser> usersByEmail = new LinkedHashMap<>();
    private final Map<String, Restaurant> restaurantsById = new LinkedHashMap<>();
    private final DemoRestaurantRepository demoRestaurantRepository;

    public AuthRepository(PasswordEncoder passwordEncoder, DemoRestaurantRepository demoRestaurantRepository) {
        this.demoRestaurantRepository = demoRestaurantRepository;
        Restaurant demo = demoRestaurantRepository.restaurant();
        restaurantsById.put(demo.id(), demo);
        usersByEmail.put(
                "owner@harborspoon.test",
                new AppUser(
                        "user-demo",
                        "owner@harborspoon.test",
                        "Sam Rivera",
                        passwordEncoder.encode("TableProof123!"),
                        List.of(demo.id())
                )
        );
    }

    public Optional<AppUser> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(normalizeEmail(email)));
    }

    public Optional<AppUser> findById(String id) {
        return usersByEmail.values().stream()
                .filter(user -> user.id().equals(id))
                .findFirst();
    }

    public AppUser createUser(String email, String name, String passwordHash, String restaurantName, String restaurantType) {
        String restaurantId = UUID.randomUUID().toString();
        Restaurant restaurant = new Restaurant(restaurantId, restaurantName, restaurantType);
        restaurantsById.put(restaurantId, restaurant);
        demoRestaurantRepository.cloneDemoTransactionsForRestaurant(restaurantId);

        AppUser user = new AppUser(
                UUID.randomUUID().toString(),
                normalizeEmail(email),
                name,
                passwordHash,
                List.of(restaurantId)
        );
        usersByEmail.put(user.email(), user);
        return user;
    }

    public List<Restaurant> restaurantsFor(AppUser user) {
        return user.restaurantIds().stream()
                .map(restaurantsById::get)
                .toList();
    }

    public boolean canAccess(AppUser user, String restaurantId) {
        return user.restaurantIds().contains(restaurantId);
    }

    public Optional<Restaurant> restaurant(String restaurantId) {
        return Optional.ofNullable(restaurantsById.get(restaurantId));
    }

    public List<AuthResponse.RestaurantAccess> restaurantAccess(AppUser user) {
        List<AuthResponse.RestaurantAccess> access = new ArrayList<>();
        for (Restaurant restaurant : restaurantsFor(user)) {
            access.add(new AuthResponse.RestaurantAccess(restaurant.id(), restaurant.name(), "Owner"));
        }
        return access;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
