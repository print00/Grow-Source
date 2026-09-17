package com.restaurantpnl.service;

import com.restaurantpnl.domain.AccountType;
import com.restaurantpnl.domain.AmountSignMode;
import com.restaurantpnl.domain.Category;
import com.restaurantpnl.domain.Confidence;
import com.restaurantpnl.domain.Direction;
import com.restaurantpnl.domain.Restaurant;
import com.restaurantpnl.domain.Transaction;
import com.restaurantpnl.domain.VendorRule;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DemoRestaurantRepository {
    private final Restaurant restaurant = new Restaurant("demo", "Harbor Spoon Cafe", "Casual dining");
    private final List<VendorRule> rules = List.of(
            new VendorRule("toast pos", Category.REVENUE, Confidence.HIGH),
            new VendorRule("sysco", Category.FOOD_BEVERAGE, Confidence.HIGH),
            new VendorRule("restaurant depot", Category.FOOD_BEVERAGE, Confidence.HIGH),
            new VendorRule("toast payroll", Category.LABOR, Confidence.HIGH),
            new VendorRule("main street properties", Category.RENT, Confidence.HIGH),
            new VendorRule("doordash", Category.DELIVERY_FEES, Confidence.MEDIUM),
            new VendorRule("uber eats", Category.DELIVERY_FEES, Confidence.MEDIUM),
            new VendorRule("utility", Category.UTILITIES, Confidence.HIGH)
    );
    private final List<Transaction> transactions = new ArrayList<>(seedTransactions());

    public Restaurant restaurant() {
        return restaurant;
    }

    public List<Transaction> transactions() {
        return transactions.stream()
                .sorted(Comparator.comparing(Transaction::date))
                .toList();
    }

    public List<Transaction> transactionsForRestaurant(String restaurantId) {
        return transactions.stream()
                .filter(transaction -> transaction.restaurantId().equals(restaurantId))
                .sorted(Comparator.comparing(Transaction::date))
                .toList();
    }

    public void cloneDemoTransactionsForRestaurant(String restaurantId) {
        List<Transaction> cloned = seedTransactions().stream()
                .map(transaction -> new Transaction(
                        UUID.randomUUID().toString(),
                        restaurantId,
                        transaction.date(),
                        transaction.vendor(),
                        transaction.description(),
                        transaction.amount(),
                        transaction.category(),
                        transaction.confidence(),
                        transaction.direction()
                ))
                .toList();
        transactions.addAll(cloned);
    }

    public List<Transaction> importCsv(String restaurantId, String csv, AccountType accountType, AmountSignMode signMode) {
        List<List<String>> rows = parseCsv(csv);
        if (rows.size() < 2) {
            return List.of();
        }

        Map<String, Integer> header = indexHeaders(rows.get(0));
        List<Transaction> imported = rows.stream()
                .skip(1)
                .filter(row -> row.stream().anyMatch(value -> !value.isBlank()))
                .map(row -> parseCsvRow(restaurantId, row, header, accountType, signMode))
                .toList();

        transactions.addAll(imported);
        return imported;
    }

    public Optional<Transaction> updateCategory(String restaurantId, String transactionId, Category category) {
        for (int index = 0; index < transactions.size(); index++) {
            Transaction current = transactions.get(index);
            if (current.id().equals(transactionId) && current.restaurantId().equals(restaurantId)) {
                Transaction updated = new Transaction(
                        current.id(),
                        current.restaurantId(),
                        current.date(),
                        current.vendor(),
                        current.description(),
                        current.amount(),
                        category,
                        Confidence.HIGH,
                        current.direction()
                );
                transactions.set(index, updated);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    private Transaction parseCsvRow(
            String restaurantId,
            List<String> row,
            Map<String, Integer> header,
            AccountType accountType,
            AmountSignMode signMode
    ) {
        LocalDate date = parseDate(firstPresent(row, header, "date", "posted_date", "transaction_date", "post_date"));
        String description = firstPresent(row, header, "description", "memo", "name", "details", "transaction");
        String vendor = firstPresent(row, header, "vendor", "merchant", "payee", "name", "description");
        if (vendor.isBlank()) {
            vendor = description;
        }
        String normalizedVendor = vendor;
        BigDecimal amount = normalizeAmount(row, header, accountType, signMode);
        VendorRule rule = rules.stream()
                .filter(candidate -> candidate.matches(normalizedVendor))
                .findFirst()
                .orElse(new VendorRule(normalizedVendor, Category.OTHER_OPERATING, Confidence.LOW));

        return new Transaction(
                UUID.randomUUID().toString(),
                restaurantId,
                date,
                vendor,
                description,
                amount,
                rule.category(),
                rule.confidence(),
                amount.signum() >= 0 ? Direction.CREDIT : Direction.DEBIT
        );
    }

    private BigDecimal normalizeAmount(
            List<String> row,
            Map<String, Integer> header,
            AccountType accountType,
            AmountSignMode signMode
    ) {
        Optional<BigDecimal> debit = firstMoney(row, header, "debit", "withdrawal", "withdrawals", "charge", "charges", "purchase", "purchases");
        Optional<BigDecimal> credit = firstMoney(row, header, "credit", "deposit", "deposits", "payment", "payments", "refund", "refunds");

        if (debit.isPresent() || credit.isPresent()) {
            BigDecimal debitAmount = debit.orElse(BigDecimal.ZERO).abs();
            BigDecimal creditAmount = credit.orElse(BigDecimal.ZERO).abs();
            if (debitAmount.compareTo(BigDecimal.ZERO) > 0) {
                return debitAmount.negate();
            }
            return creditAmount;
        }

        BigDecimal rawAmount = firstMoney(row, header, "amount", "transaction_amount", "net", "value")
                .orElseThrow(() -> new IllegalArgumentException("CSV rows need an amount column or debit/credit columns"));
        AmountSignMode effectiveMode = signMode == AmountSignMode.AUTO
                ? (accountType == AccountType.CREDIT_CARD ? AmountSignMode.MONEY_OUT_POSITIVE : AmountSignMode.MONEY_IN_POSITIVE)
                : signMode;

        return effectiveMode == AmountSignMode.MONEY_OUT_POSITIVE
                ? rawAmount.negate()
                : rawAmount;
    }

    private List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < csv.length(); index++) {
            char current = csv.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                row.add(value.toString().trim());
                value.setLength(0);
            } else if ((current == '\n' || current == '\r') && !quoted) {
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
                    index++;
                }
                row.add(value.toString().trim());
                value.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                value.append(current);
            }
        }

        if (!value.isEmpty() || !row.isEmpty()) {
            row.add(value.toString().trim());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Integer> indexHeaders(List<String> headers) {
        Map<String, Integer> indexed = new HashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            indexed.put(normalizeHeader(headers.get(index)), index);
        }
        return indexed;
    }

    private String normalizeHeader(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private String firstPresent(List<String> row, Map<String, Integer> header, String... names) {
        for (String name : names) {
            Integer index = header.get(name);
            if (index != null && index < row.size() && !row.get(index).isBlank()) {
                return row.get(index).trim();
            }
        }
        return "";
    }

    private Optional<BigDecimal> firstMoney(List<String> row, Map<String, Integer> header, String... names) {
        String value = firstPresent(row, header, names);
        if (value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(parseMoney(value));
    }

    private BigDecimal parseMoney(String value) {
        String cleaned = value.trim()
                .replace("$", "")
                .replace(",", "")
                .replace(" ", "");
        boolean parenthesesNegative = cleaned.startsWith("(") && cleaned.endsWith(")");
        cleaned = cleaned.replace("(", "").replace(")", "");
        BigDecimal amount = new BigDecimal(cleaned);
        return parenthesesNegative ? amount.negate() : amount;
    }

    private LocalDate parseDate(String value) {
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M-d-yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (RuntimeException ignored) {
                // Try the next common bank-export format.
            }
        }
        throw new IllegalArgumentException("Unsupported date format: " + value);
    }

    private List<Transaction> seedTransactions() {
        return List.of(
                tx("t1", "2026-09-02", "Toast POS Deposit", "Card sales deposit", "18420.00", Category.REVENUE, Confidence.HIGH),
                tx("t2", "2026-09-03", "Sysco", "Weekly produce and proteins", "-5210.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("t3", "2026-09-04", "Restaurant Depot", "Dry goods and supplies", "-3180.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("t4", "2026-09-05", "Toast Payroll", "Biweekly payroll", "-11240.00", Category.LABOR, Confidence.HIGH),
                tx("t5", "2026-09-09", "DoorDash", "Marketplace commission", "-1390.00", Category.DELIVERY_FEES, Confidence.MEDIUM),
                tx("t6", "2026-09-12", "Beltway Utility", "Gas and electric", "-1180.00", Category.UTILITIES, Confidence.HIGH),
                tx("t7", "2026-09-16", "QuickSupply Co", "Unclear restaurant supplies", "-740.00", Category.OTHER_OPERATING, Confidence.LOW),
                tx("t8", "2026-09-19", "Toast POS Deposit", "Card sales deposit", "19870.00", Category.REVENUE, Confidence.HIGH),
                tx("t9", "2026-09-21", "Main Street Properties", "Restaurant lease", "-7500.00", Category.RENT, Confidence.HIGH),
                tx("t10", "2026-09-22", "Sysco", "Proteins and dairy", "-3910.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("t11", "2026-09-23", "Uber Eats", "Delivery marketplace commission", "-1380.00", Category.DELIVERY_FEES, Confidence.MEDIUM),
                tx("t12", "2026-09-24", "Toast Payroll", "Biweekly payroll", "-11600.00", Category.LABOR, Confidence.HIGH),
                tx("t13", "2026-09-26", "Toast POS Deposit", "Card sales deposit", "21980.00", Category.REVENUE, Confidence.HIGH),
                tx("t14", "2026-09-27", "DoorDash", "Marketplace commission", "-1350.00", Category.DELIVERY_FEES, Confidence.MEDIUM),
                tx("t15", "2026-09-28", "Restaurant Depot", "Paper goods and dry stock", "-3100.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("t16", "2026-09-29", "Toast POS Deposit", "Cash and card deposit", "22180.00", Category.REVENUE, Confidence.HIGH),
                tx("t17", "2026-09-29", "Beltway Utility", "Water and trash", "-1000.00", Category.UTILITIES, Confidence.HIGH),
                tx("t18", "2026-09-30", "Local Repair", "Dishwasher repair", "-5600.00", Category.OTHER_OPERATING, Confidence.LOW),
                tx("t19", "2026-09-30", "Sysco", "Month-end proteins and produce", "-5900.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("t20", "2026-09-30", "Restaurant Depot", "Beverage and dry stock", "-3890.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a1", "2026-08-03", "Toast POS Deposit", "Card sales deposit", "19240.00", Category.REVENUE, Confidence.HIGH),
                tx("a2", "2026-08-04", "Sysco", "Weekly produce and proteins", "-3460.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a3", "2026-08-05", "Restaurant Depot", "Dry goods and supplies", "-2140.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a4", "2026-08-05", "Toast Payroll", "Biweekly payroll", "-10680.00", Category.LABOR, Confidence.HIGH),
                tx("a5", "2026-08-10", "DoorDash", "Marketplace commission", "-1120.00", Category.DELIVERY_FEES, Confidence.MEDIUM),
                tx("a6", "2026-08-16", "Toast POS Deposit", "Card sales deposit", "18380.00", Category.REVENUE, Confidence.HIGH),
                tx("a7", "2026-08-20", "Main Street Properties", "Restaurant lease", "-7500.00", Category.RENT, Confidence.HIGH),
                tx("a8", "2026-08-22", "Toast Payroll", "Biweekly payroll", "-10710.00", Category.LABOR, Confidence.HIGH),
                tx("a9", "2026-08-24", "Toast POS Deposit", "Card sales deposit", "19620.00", Category.REVENUE, Confidence.HIGH),
                tx("a10", "2026-08-25", "Uber Eats", "Delivery marketplace commission", "-1180.00", Category.DELIVERY_FEES, Confidence.MEDIUM),
                tx("a11", "2026-08-27", "Beltway Utility", "Gas and electric", "-1990.00", Category.UTILITIES, Confidence.HIGH),
                tx("a12", "2026-08-30", "Toast POS Deposit", "Cash and card deposit", "18970.00", Category.REVENUE, Confidence.HIGH),
                tx("a13", "2026-08-30", "Local Repair", "Equipment repair", "-6540.00", Category.OTHER_OPERATING, Confidence.LOW),
                tx("a14", "2026-08-31", "Sysco", "Produce and dairy", "-4240.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a15", "2026-08-31", "Restaurant Depot", "Paper goods", "-2350.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a16", "2026-08-31", "Sysco", "Month-end proteins and dairy", "-4100.00", Category.FOOD_BEVERAGE, Confidence.HIGH),
                tx("a17", "2026-08-31", "Restaurant Depot", "Beverage and dry stock", "-5000.00", Category.FOOD_BEVERAGE, Confidence.HIGH)
        );
    }

    private Transaction tx(String id, String date, String vendor, String description, String amount, Category category, Confidence confidence) {
        BigDecimal parsedAmount = new BigDecimal(amount);
        return new Transaction(
                id,
                restaurant.id(),
                LocalDate.parse(date),
                vendor,
                description,
                parsedAmount,
                category,
                confidence,
                parsedAmount.signum() >= 0 ? Direction.CREDIT : Direction.DEBIT
        );
    }
}
