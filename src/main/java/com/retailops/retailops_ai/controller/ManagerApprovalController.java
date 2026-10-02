package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.ApprovalDecisionRequest;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.service.ApprovalService;
import com.retailops.retailops_ai.service.RecommendationApprovalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ManagerApprovalController {

    private final ManagerApprovalRepository approvalRepository;
    private final ApprovalService approvalService;
    private final RecommendationApprovalService recommendationApprovalService;

    public ManagerApprovalController(
            ManagerApprovalRepository approvalRepository,
            ApprovalService approvalService,
            RecommendationApprovalService recommendationApprovalService) {
        this.approvalRepository = approvalRepository;
        this.approvalService = approvalService;
        this.recommendationApprovalService = recommendationApprovalService;
    }

    @GetMapping
    public List<ManagerApproval> getApprovals() {
        return approvalRepository.findAll();
    }

    @GetMapping("/{actionId}")
    public ManagerApproval getApprovalById(@PathVariable String actionId) {
        return approvalRepository.findById(actionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
    }

    @PutMapping("/{actionId}/approve")
    public ManagerApproval approve(
            @PathVariable String actionId,
            @RequestBody ApprovalDecisionRequest request) {
        return approvalService.decide(actionId, "APPROVED", request);
    }

    @PutMapping("/{actionId}/reject")
    public ManagerApproval reject(
            @PathVariable String actionId,
            @RequestBody ApprovalDecisionRequest request) {
        return approvalService.decide(actionId, "REJECTED", request);
    }

    @PostMapping("/generate")
    public ResponseEntity<List<ManagerApproval>> createPendingApprovals() {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationApprovalService.createPendingApprovals());
    }
}