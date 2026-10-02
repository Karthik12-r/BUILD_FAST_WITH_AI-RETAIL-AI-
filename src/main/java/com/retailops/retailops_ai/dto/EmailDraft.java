package com.retailops.retailops_ai.dto;

public record EmailDraft(
        boolean eligible,
        String to,
        String subject,
        String message,
        String reason) {
}