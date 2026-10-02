package com.retailops.retailops_ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BranchSummary(
        String id,
        String name,
        List<String> storeIds,
        LocalDate dataAsOf,
        BigDecimal latestDayRevenue,
        long latestDayUnitsSold,
        int healthyItems,
        int lowStockItems,
        int outOfStockItems,
        long pendingApprovals,
        String status) {
}