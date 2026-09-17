package com.restaurantpnl.service;

import com.restaurantpnl.api.DashboardResponse;
import com.restaurantpnl.domain.Category;
import com.restaurantpnl.domain.CategoryGroup;
import com.restaurantpnl.domain.Confidence;
import com.restaurantpnl.domain.Restaurant;
import com.restaurantpnl.domain.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinancialCalculator {
    public DashboardResponse dashboard(Restaurant restaurant, List<Transaction> allTransactions, YearMonth month) {
        List<Transaction> current = transactionsForMonth(allTransactions, month);
        List<Transaction> previous = transactionsForMonth(allTransactions, month.minusMonths(1));

        BigDecimal revenue = totalByCategoryGroup(current, CategoryGroup.REVENUE);
        BigDecimal foodCost = totalByCategory(current, Category.FOOD_BEVERAGE).abs();
        BigDecimal laborCost = totalByCategory(current, Category.LABOR).abs();
        BigDecimal rent = totalByCategory(current, Category.RENT).abs();
        BigDecimal delivery = totalByCategory(current, Category.DELIVERY_FEES).abs();
        BigDecimal utilities = totalByCategory(current, Category.UTILITIES).abs();
        BigDecimal other = totalByCategory(current, Category.OTHER_OPERATING).abs();
        BigDecimal expenses = foodCost.add(laborCost).add(rent).add(delivery).add(utilities).add(other);
        BigDecimal netProfit = revenue.subtract(expenses);
        BigDecimal primeCost = foodCost.add(laborCost);

        BigDecimal previousRevenue = totalByCategoryGroup(previous, CategoryGroup.REVENUE);
        BigDecimal previousExpenses = totalExpenses(previous);
        BigDecimal previousProfit = previousRevenue.subtract(previousExpenses);

        DashboardResponse.Metrics metrics = new DashboardResponse.Metrics(
                dollars(revenue),
                dollars(expenses),
                dollars(netProfit),
                percent(netProfit, revenue),
                dollars(foodCost),
                percent(foodCost, revenue),
                dollars(laborCost),
                percent(laborCost, revenue),
                dollars(primeCost),
                percent(primeCost, revenue),
                dollars(revenue.divide(BigDecimal.valueOf(month.lengthOfMonth()), 2, RoundingMode.HALF_UP)),
                percentChange(revenue, previousRevenue),
                percentChange(netProfit, previousProfit)
        );

        return new DashboardResponse(
                new DashboardResponse.RestaurantSummary(restaurant.id(), restaurant.name(), restaurant.type(), monthLabel(month)),
                metrics,
                moneyFlow(revenue, netProfit, foodCost, laborCost, rent, delivery, utilities.add(other)),
                topVendors(current, previous),
                transactions(current),
                pnlLines(current, previous),
                insights(metrics, topVendors(current, previous)),
                List.of(
                        "Why did profit fall if sales were up?",
                        "Which costs should I look at first?",
                        "Is my labor cost healthy?",
                        "What changed with food vendors?"
                )
        );
    }

    private List<Transaction> transactionsForMonth(List<Transaction> transactions, YearMonth month) {
        return transactions.stream()
                .filter(transaction -> YearMonth.from(transaction.date()).equals(month))
                .toList();
    }

    private BigDecimal totalByCategoryGroup(List<Transaction> transactions, CategoryGroup group) {
        return transactions.stream()
                .filter(transaction -> transaction.category().group() == group)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalByCategory(List<Transaction> transactions, Category category) {
        return transactions.stream()
                .filter(transaction -> transaction.category() == category)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.category().group() != CategoryGroup.REVENUE)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();
    }

    private List<DashboardResponse.MoneyFlowItem> moneyFlow(
            BigDecimal revenue,
            BigDecimal netProfit,
            BigDecimal foodCost,
            BigDecimal laborCost,
            BigDecimal rent,
            BigDecimal delivery,
            BigDecimal other
    ) {
        return List.of(
                flow("Profit", netProfit, revenue, "profit"),
                flow("Food & beverage", foodCost, revenue, "food"),
                flow("Labor", laborCost, revenue, "labor"),
                flow("Rent", rent, revenue, "rent"),
                flow("Delivery fees", delivery, revenue, "delivery"),
                flow("Other", other, revenue, "other")
        );
    }

    private DashboardResponse.MoneyFlowItem flow(String label, BigDecimal amount, BigDecimal revenue, String tone) {
        return new DashboardResponse.MoneyFlowItem(label, dollars(amount), percent(amount, revenue), tone);
    }

    private List<DashboardResponse.VendorSummary> topVendors(List<Transaction> current, List<Transaction> previous) {
        Map<String, BigDecimal> previousByVendor = previous.stream()
                .filter(transaction -> transaction.category().group() != CategoryGroup.REVENUE)
                .collect(Collectors.groupingBy(
                        Transaction::vendor,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::absoluteAmount, BigDecimal::add)
                ));

        return current.stream()
                .filter(transaction -> transaction.category().group() != CategoryGroup.REVENUE)
                .collect(Collectors.groupingBy(
                        Transaction::vendor,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::absoluteAmount, BigDecimal::add)
                ))
                .entrySet()
                .stream()
                .map(entry -> {
                    Category category = current.stream()
                            .filter(transaction -> transaction.vendor().equals(entry.getKey()))
                            .findFirst()
                            .map(Transaction::category)
                            .orElse(Category.OTHER_OPERATING);
                    BigDecimal previousAmount = previousByVendor.getOrDefault(entry.getKey(), BigDecimal.ZERO);
                    return new DashboardResponse.VendorSummary(
                            entry.getKey(),
                            category.label(),
                            dollars(entry.getValue()),
                            percentChange(entry.getValue(), previousAmount)
                    );
                })
                .sorted(Comparator.comparing(DashboardResponse.VendorSummary::amount).reversed())
                .limit(5)
                .toList();
    }

    private List<DashboardResponse.TransactionSummary> transactions(List<Transaction> current) {
        return current.stream()
                .map(transaction -> new DashboardResponse.TransactionSummary(
                        transaction.id(),
                        transaction.date().toString(),
                        transaction.vendor(),
                        transaction.description(),
                        dollars(transaction.amount()),
                        transaction.category().label(),
                        transaction.confidence().name().toLowerCase(Locale.US),
                        transaction.direction().name().toLowerCase(Locale.US)
                ))
                .toList();
    }

    private List<DashboardResponse.PnlLine> pnlLines(List<Transaction> current, List<Transaction> previous) {
        return List.of(
                pnlLine("Sales", Category.REVENUE, current, previous),
                pnlLine("Food & beverage", Category.FOOD_BEVERAGE, current, previous),
                pnlLine("Labor", Category.LABOR, current, previous),
                pnlLine("Rent", Category.RENT, current, previous),
                pnlLine("Delivery fees", Category.DELIVERY_FEES, current, previous),
                pnlLine("Utilities", Category.UTILITIES, current, previous),
                pnlLine("Other operating", Category.OTHER_OPERATING, current, previous)
        );
    }

    private DashboardResponse.PnlLine pnlLine(String label, Category category, List<Transaction> current, List<Transaction> previous) {
        BigDecimal currentAmount = totalByCategory(current, category);
        BigDecimal previousAmount = totalByCategory(previous, category);
        return new DashboardResponse.PnlLine(
                label,
                dollars(currentAmount),
                category.group().name().toLowerCase(Locale.US),
                percentChange(currentAmount.abs(), previousAmount.abs())
        );
    }

    private List<DashboardResponse.Insight> insights(DashboardResponse.Metrics metrics, List<DashboardResponse.VendorSummary> vendors) {
        DashboardResponse.VendorSummary topVendor = vendors.isEmpty()
                ? new DashboardResponse.VendorSummary("No vendor", "Other", 0, 0)
                : vendors.get(0);

        return List.of(
                new DashboardResponse.Insight(
                        "Sales rose, but profit slipped",
                        "Revenue increased " + signed(metrics.revenueChangePercent()) + " from last month, while net profit changed " + signed(metrics.profitChangePercent()) + ". Food, delivery, and repair costs explain the pressure.",
                        "warning"
                ),
                new DashboardResponse.Insight(
                        "Food cost is above target",
                        "Food cost is " + rounded(metrics.foodCostPercent()) + "% of sales. For many restaurants, the target is closer to 28%.",
                        metrics.foodCostPercent() > 30 ? "alert" : "good"
                ),
                new DashboardResponse.Insight(
                        "Prime cost is the main health check",
                        "Food plus labor is " + rounded(metrics.primeCostPercent()) + "% of sales. The first version treats this as the owner dashboard's north-star cost metric.",
                        metrics.primeCostPercent() <= 60 ? "good" : "warning"
                ),
                new DashboardResponse.Insight(
                        topVendor.name() + " is the largest vendor",
                        topVendor.name() + " totaled $" + Math.round(topVendor.amount()) + " and changed " + signed(topVendor.changePercent()) + " month over month.",
                        topVendor.changePercent() > 20 ? "warning" : "good"
                )
        );
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
    }

    private double dollars(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private double percent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0 : 100;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String signed(double value) {
        return (value > 0 ? "+" : "") + rounded(value) + "%";
    }

    private String rounded(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString();
    }
}
