package com.retailops.retailops_ai.dto;

import java.time.LocalDate;

public record ReorderRecommendation(
        String productId,
        String storeId,
        String action,
        int recommendedQuantity,
        String reason,
        String priority,
        int inventoryLevel,
        int dailyDemand,
        int leadTimeDays,
        int minimumOrderQuantity,
        LocalDate asOf) {
}