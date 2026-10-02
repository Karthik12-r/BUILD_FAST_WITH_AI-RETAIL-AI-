package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.ReorderRecommendation;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationApprovalService {

    private final RecommendationService recommendationService;
    private final ManagerApprovalRepository approvalRepository;
    private final TransactionTemplate transactionTemplate;

    public RecommendationApprovalService(
            RecommendationService recommendationService,
            ManagerApprovalRepository approvalRepository,
            PlatformTransactionManager transactionManager) {
        this.recommendationService = recommendationService;
        this.approvalRepository = approvalRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public synchronized List<ManagerApproval> createPendingApprovals() {
        return transactionTemplate.execute(status -> {
            List<ManagerApproval> approvals = recommendationService.getReorderRecommendations().stream()
                    .map(this::toApproval)
                    .toList();
            return approvalRepository.saveAll(approvals);
        });
    }

    private ManagerApproval toApproval(ReorderRecommendation recommendation) {
        ManagerApproval approval = new ManagerApproval();
        approval.setActionId("ACT" + UUID.randomUUID().toString().replace("-", "").substring(0, 27));
        approval.setApprovalDate(LocalDate.now());
        approval.setStoreId(recommendation.storeId());
        approval.setProductId(recommendation.productId());
        approval.setActionType(recommendation.action());
        approval.setRecommendedQuantity(recommendation.recommendedQuantity());
        approval.setReason(recommendation.reason());
        approval.setStatus("PENDING");
        return approval;
    }
}