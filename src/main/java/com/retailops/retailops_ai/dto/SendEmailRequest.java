package com.retailops.retailops_ai.dto;

public record SendEmailRequest(String to, String subject, String message) {
}