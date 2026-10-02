package com.retailops.retailops_ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "manager_approvals")
public class ManagerApproval {

    @Id
    @Column(name = "action_id")
    private String actionId;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    @Column(name = "store_id")
    private String storeId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "recommended_quantity")
    private Integer recommendedQuantity;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "status")
    private String status;

    @Column(name = "approver")
    private String approver;

    @Column(name = "approval_timestamp")
    private LocalDateTime approvalTimestamp;

    @Column(name = "manager_comment", columnDefinition = "TEXT")
    private String managerComment;

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getRecommendedQuantity() {
        return recommendedQuantity;
    }

    public void setRecommendedQuantity(Integer recommendedQuantity) {
        this.recommendedQuantity = recommendedQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public LocalDateTime getApprovalTimestamp() {
        return approvalTimestamp;
    }

    public void setApprovalTimestamp(LocalDateTime approvalTimestamp) {
        this.approvalTimestamp = approvalTimestamp;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }
}