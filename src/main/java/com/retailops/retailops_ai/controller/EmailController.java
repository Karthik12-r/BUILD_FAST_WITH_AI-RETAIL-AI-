package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.dto.EmailDraft;
import com.retailops.retailops_ai.dto.EmailSendResponse;
import com.retailops.retailops_ai.dto.SendEmailRequest;
import com.retailops.retailops_ai.service.EmailService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/out-of-stock/{actionId}")
    public EmailDraft createOutOfStockDraft(@PathVariable String actionId) {
        return emailService.createOutOfStockDraft(actionId);
    }

    @PostMapping("/send")
    public EmailSendResponse sendEmail(@RequestBody SendEmailRequest request) {
        return emailService.sendEmail(request);
    }
}