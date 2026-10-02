package com.retailops.retailops_ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BranchOverview(
        BranchSummary branch,
        String period,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal revenue,
        BigDecimal previousRevenue,
        BigDecimal revenueChangePercent,
        long unitsSold,
        long previousUnitsSold,
        BigDecimal averageSalesRowValue,
        BigDecimal inventoryValue,
        int healthyItems,
        int lowStockItems,
        int outOfStockItems,
        long pendingApprovals,
        BigDecimal performanceBaseline,
        BigDecimal achievementPercent,
        List<RevenuePoint> salesTrend,
        List<ProductPerformance> topProducts,
        List<RecentOperation> recentOperations,
        String insight) {

    public record RevenuePoint(LocalDate date, BigDecimal revenue, BigDecimal previousRevenue) {
    }

    public record ProductPerformance(
            String productId,
            String productName,
            String category,
            long unitsSold,
            BigDecimal revenue) {
    }

    public record RecentOperation(String type, String detail, String status, String timestamp) {
    }
}