package com.restaurantpnl.api;

import com.restaurantpnl.domain.Category;
import com.restaurantpnl.domain.AccountType;
import com.restaurantpnl.domain.AmountSignMode;
import com.restaurantpnl.domain.Transaction;
import com.restaurantpnl.auth.AppUser;
import com.restaurantpnl.auth.AuthRepository;
import com.restaurantpnl.service.DemoRestaurantRepository;
import com.restaurantpnl.service.FinancialCalculator;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/restaurants")
@CrossOrigin(origins = "http://localhost:3000")
public class RestaurantController {
    private final DemoRestaurantRepository repository;
    private final AuthRepository authRepository;
    private final FinancialCalculator calculator;

    public RestaurantController(DemoRestaurantRepository repository, AuthRepository authRepository, FinancialCalculator calculator) {
        this.repository = repository;
        this.authRepository = authRepository;
        this.calculator = calculator;
    }

    @GetMapping("/{restaurantId}/dashboard")
    public DashboardResponse dashboard(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "2026-09") String month,
            @AuthenticationPrincipal AppUser user
    ) {
        requireAccess(user, restaurantId);
        return calculator.dashboard(
                authRepository.restaurant(restaurantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)),
                repository.transactionsForRestaurant(restaurantId),
                YearMonth.parse(month)
        );
    }

    @GetMapping("/{restaurantId}/transactions")
    public List<DashboardResponse.TransactionSummary> transactions(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "2026-09") String month,
            @AuthenticationPrincipal AppUser user
    ) {
        return dashboard(restaurantId, month, user).transactions();
    }

    @PostMapping(path = "/{restaurantId}/import-csv", consumes = "text/csv")
    public List<Transaction> importCsv(
            @PathVariable String restaurantId,
            @RequestParam(defaultValue = "BANK") AccountType accountType,
            @RequestParam(defaultValue = "AUTO") AmountSignMode signMode,
            @RequestBody String csv,
            @AuthenticationPrincipal AppUser user
    ) {
        requireAccess(user, restaurantId);
        return repository.importCsv(restaurantId, csv, accountType, signMode);
    }

    @PutMapping("/{restaurantId}/transactions/{transactionId}/category")
    public ResponseEntity<Transaction> updateCategory(
            @PathVariable String restaurantId,
            @PathVariable String transactionId,
            @RequestBody UpdateCategoryRequest request,
            @AuthenticationPrincipal AppUser user
    ) {
        requireAccess(user, restaurantId);
        Category category = Category.valueOf(request.category().trim().toUpperCase(Locale.US));
        return repository.updateCategory(restaurantId, transactionId, category)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void requireAccess(AppUser user, String restaurantId) {
        if (user == null || !authRepository.canAccess(user, restaurantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this restaurant");
        }
    }

    public record UpdateCategoryRequest(@NotBlank String category) {
    }
}
