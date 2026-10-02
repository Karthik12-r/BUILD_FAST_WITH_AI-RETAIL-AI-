package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.ReorderRecommendation;
import com.retailops.retailops_ai.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<ReorderRecommendation> getRecommendations() {
        return recommendationService.getReorderRecommendations();
    }
}