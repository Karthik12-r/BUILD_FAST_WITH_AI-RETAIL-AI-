package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.SendEmailRequest;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.entity.Supplier;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private final ManagerApprovalRepository approvalRepository = mock(ManagerApprovalRepository.class);
    private final SalesDataRepository salesDataRepository = mock(SalesDataRepository.class);
    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(
            approvalRepository, salesDataRepository, supplierRepository, mailSender,
            new ProductNameCatalog(), "retailops@example.com");
    }

    @Test
    void createsDraftFromCurrentOutOfStockDataAndSupplier() {
        ManagerApproval approval = approval("P0002", "S001");
        SalesData sale = sale("P0002", "S001", 0, "Clothing");
        Supplier supplier = new Supplier();
        supplier.setEmail("supplier@example.com");
        when(approvalRepository.findById("ACT1")).thenReturn(Optional.of(approval));
        when(salesDataRepository.findFirstByStoreIdAndProductIdOrderByDateDescIdDesc("S001", "P0002"))
                .thenReturn(Optional.of(sale));
        when(supplierRepository.findAllByProductId("P0002")).thenReturn(List.of(supplier));

        var draft = emailService.createOutOfStockDraft("ACT1");

        assertThat(draft.eligible()).isTrue();
        assertThat(draft.to()).isEqualTo("supplier@example.com");
        assertThat(draft.subject()).isEqualTo("Urgent: Willow and Weft Cotton Crew Tee Out of Stock");
        assertThat(draft.message()).contains("currently out of stock", "Willow and Weft Cotton Crew Tee");
    }

    @Test
    void generatesDraftForNonzeroInventoryOnApproval() {
        when(approvalRepository.findById("ACT1")).thenReturn(Optional.of(approval("P0002", "S001")));
        when(salesDataRepository.findFirstByStoreIdAndProductIdOrderByDateDescIdDesc("S001", "P0002"))
                .thenReturn(Optional.of(sale("P0002", "S001", 2, "Clothing")));
        Supplier supplier = new Supplier();
        supplier.setEmail("supplier@example.com");
        when(supplierRepository.findAllByProductId("P0002")).thenReturn(List.of(supplier));

        var draft = emailService.createOutOfStockDraft("ACT1");

        assertThat(draft.eligible()).isTrue();
        assertThat(draft.to()).isEqualTo("supplier@example.com");
        assertThat(draft.message()).contains("currently out of stock");
    }

    @Test
    void rejectsInvalidRecipientWithoutSending() {
        assertThatThrownBy(() -> emailService.sendEmail(
                new SendEmailRequest("not-an-email", "Subject", "Message")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void rejectsEmptySubjectAndMessageWithoutSending() {
        assertThatThrownBy(() -> emailService.sendEmail(
                new SendEmailRequest("supplier@example.com", "  ", "Message")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThatThrownBy(() -> emailService.sendEmail(
                new SendEmailRequest("supplier@example.com", "Subject", "  ")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void reportsMailServerFailureAsGatewayError() {
        doThrow(new MailSendException("SMTP unavailable"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendEmail(
                new SendEmailRequest("supplier@example.com", "Subject", "Message")))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void returnsSuccessAfterMailSenderAcceptsMessage() {
        var response = emailService.sendEmail(
                new SendEmailRequest("supplier@example.com", "Subject", "Message"));

        assertThat(response.message()).isEqualTo("Email sent successfully to supplier@example.com");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    private ManagerApproval approval(String productId, String storeId) {
        ManagerApproval approval = new ManagerApproval();
        approval.setActionId("ACT1");
        approval.setProductId(productId);
        approval.setStoreId(storeId);
        approval.setStatus("PENDING");
        return approval;
    }

    private SalesData sale(String productId, String storeId, int inventory, String category) {
        SalesData sale = new SalesData();
        sale.setProductId(productId);
        sale.setStoreId(storeId);
        sale.setInventoryLevel(inventory);
        sale.setCategory(category);
        return sale;
    }
}