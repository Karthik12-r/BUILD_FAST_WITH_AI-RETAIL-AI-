package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.BranchOverview;
import com.retailops.retailops_ai.dto.BranchSummary;
import com.retailops.retailops_ai.service.BranchAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchAnalyticsService branchAnalyticsService;

    public BranchController(BranchAnalyticsService branchAnalyticsService) {
        this.branchAnalyticsService = branchAnalyticsService;
    }

    @GetMapping
    public List<BranchSummary> getBranches() {
        return branchAnalyticsService.getBranches();
    }

    @GetMapping("/{branchId}/overview")
    public BranchOverview getBranchOverview(
            @PathVariable String branchId,
            @RequestParam(defaultValue = "30D") String period) {
        return branchAnalyticsService.getOverview(branchId, period);
    }
}