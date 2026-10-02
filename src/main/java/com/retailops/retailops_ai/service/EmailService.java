package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.EmailDraft;
import com.retailops.retailops_ai.dto.EmailSendResponse;
import com.retailops.retailops_ai.dto.SendEmailRequest;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.entity.Supplier;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailService {

    private final ManagerApprovalRepository approvalRepository;
    private final SalesDataRepository salesDataRepository;
    private final SupplierRepository supplierRepository;
    private final JavaMailSender mailSender;
    private final ProductNameCatalog productNameCatalog;
    private final String fromAddress;

    public EmailService(
            ManagerApprovalRepository approvalRepository,
            SalesDataRepository salesDataRepository,
            SupplierRepository supplierRepository,
            JavaMailSender mailSender,
            ProductNameCatalog productNameCatalog,
            @Value("${retailops.email.from:}") String fromAddress) {
        this.approvalRepository = approvalRepository;
        this.salesDataRepository = salesDataRepository;
        this.supplierRepository = supplierRepository;
        this.mailSender = mailSender;
        this.productNameCatalog = productNameCatalog;
        this.fromAddress = fromAddress;
    }

    public EmailDraft createOutOfStockDraft(String actionId) {
        ManagerApproval approval = approvalRepository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
        if (!"PENDING".equalsIgnoreCase(approval.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending approvals can create an email draft");
        }

        SalesData sale = salesDataRepository
                .findFirstByStoreIdAndProductIdOrderByDateDescIdDesc(approval.getStoreId(), approval.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Current inventory record not found"));

        Supplier supplier = supplierRepository.findAllByProductId(approval.getProductId()).stream()
                .filter(item -> item.getEmail() != null && !item.getEmail().isBlank())
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "No supplier email is available for this product"));

        String productName = productName(sale);
        String message = "Dear Supplier,\n\n"
                + "The product " + productName + " is currently out of stock in our inventory.\n"
                + "Please arrange replenishment at the earliest to avoid disruption to sales.\n\n"
                + "Regards,\nRetailOps AI";
        return new EmailDraft(
                true,
                supplier.getEmail(),
                "Urgent: " + productName + " Out of Stock",
                message,
                null);
    }

    public EmailSendResponse sendEmail(SendEmailRequest request) {
        if (request == null || request.to() == null || request.to().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A recipient email address is required");
        }
        if (request.subject() == null || request.subject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email subject is required");
        }
        if (request.subject().contains("\r") || request.subject().contains("\n")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email subject must be a single line");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email message is required");
        }

        String to = request.to().trim();
        try {
            InternetAddress address = new InternetAddress(to, true);
            address.validate();
        } catch (AddressException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid recipient email address");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Email is not configured; set MAIL_USERNAME and MAIL_PASSWORD");
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(to);
        mail.setSubject(request.subject().trim());
        mail.setText(request.message());
        try {
            mailSender.send(mail);
        } catch (MailException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Email could not be sent; check SMTP settings and try again");
        }
        return new EmailSendResponse("Email sent successfully to " + to);
    }

    private String productName(SalesData sale) {
        return productNameCatalog.displayName(sale.getProductId(), sale.getCategory());
    }
}