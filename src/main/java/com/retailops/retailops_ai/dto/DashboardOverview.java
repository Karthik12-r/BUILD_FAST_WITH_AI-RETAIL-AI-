package com.retailops.retailops_ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardOverview(
        BigDecimal totalRevenue,
        long totalUnitsSold,
        long reorderAlerts,
        long pendingApprovals,
        long supplierCount,
        List<ProductSales> topProducts,
        List<MonthlySales> salesTrend) {

    public record ProductSales(String productId, long unitsSold) {
    }

    public record MonthlySales(String month, BigDecimal revenue, long unitsSold) {
    }
}