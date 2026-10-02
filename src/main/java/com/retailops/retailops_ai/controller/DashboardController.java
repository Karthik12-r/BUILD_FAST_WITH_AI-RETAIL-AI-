package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.DashboardOverview;
import com.retailops.retailops_ai.service.DashboardAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class DashboardController {

    private final DashboardAnalyticsService analyticsService;

    public DashboardController(DashboardAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public DashboardOverview getDashboardOverview() {
        return analyticsService.getOverview();
    }
}