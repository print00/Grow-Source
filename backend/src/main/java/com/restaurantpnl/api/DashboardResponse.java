package com.restaurantpnl.api;

import java.util.List;

public record DashboardResponse(
        RestaurantSummary restaurant,
        Metrics metrics,
        List<MoneyFlowItem> moneyFlow,
        List<VendorSummary> topVendors,
        List<TransactionSummary> transactions,
        List<PnlLine> pnlLines,
        List<Insight> insights,
        List<String> questions
) {
    public record RestaurantSummary(String id, String name, String type, String month) {
    }

    public record Metrics(
            double revenue,
            double expenses,
            double netProfit,
            double profitMargin,
            double foodCost,
            double foodCostPercent,
            double laborCost,
            double laborCostPercent,
            double primeCost,
            double primeCostPercent,
            double averageDailySales,
            double revenueChangePercent,
            double profitChangePercent
    ) {
    }

    public record MoneyFlowItem(String label, double amount, double percentOfRevenue, String tone) {
    }

    public record VendorSummary(String name, String category, double amount, double changePercent) {
    }

    public record TransactionSummary(
            String id,
            String date,
            String vendor,
            String description,
            double amount,
            String category,
            String confidence,
            String direction
    ) {
    }

    public record PnlLine(String label, double amount, String kind, double changePercent) {
    }

    public record Insight(String title, String detail, String severity) {
    }
}
