package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.ApprovalDecisionRequest;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class ApprovalService {

    private final ManagerApprovalRepository approvalRepository;

    public ApprovalService(ManagerApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    @Transactional
    public ManagerApproval decide(String actionId, String decision, ApprovalDecisionRequest request) {
        String approver = request.getApprover() == null ? "" : request.getApprover().trim();
        String managerComment = request.getManagerComment() == null ? "" : request.getManagerComment().trim();

        if (approver.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "approver is required");
        }
        if ("REJECTED".equals(decision) && managerComment.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "managerComment is required when rejecting");
        }

        ManagerApproval approval = approvalRepository.findByActionIdForUpdate(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!"PENDING".equalsIgnoreCase(approval.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending approvals can be decided");
        }

        approval.setStatus(decision);
        approval.setApprover(approver);
        approval.setApprovalTimestamp(LocalDateTime.now());
        if (!managerComment.isEmpty()) {
            approval.setManagerComment(managerComment);
        }
        return approvalRepository.save(approval);
    }
}