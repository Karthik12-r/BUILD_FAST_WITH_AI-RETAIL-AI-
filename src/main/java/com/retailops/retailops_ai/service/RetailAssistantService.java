package com.retailops.retailops_ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.retailops.retailops_ai.dto.AssistantAnswer;
import com.retailops.retailops_ai.dto.AssistantQuestion;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class RetailAssistantService {

    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DashboardAnalyticsService dashboardService;
    private final RecommendationService recommendationService;
    private final ManagerApprovalRepository approvalRepository;
    private final SupplierRepository supplierRepository;

    public RetailAssistantService(
            @Value("${retailops.ai.api-url}") String apiUrl,
            @Value("${retailops.ai.api-key}") String apiKey,
            @Value("${retailops.ai.model}") String model,
            ObjectMapper objectMapper,
            DashboardAnalyticsService dashboardService,
            RecommendationService recommendationService,
            ManagerApprovalRepository approvalRepository,
            SupplierRepository supplierRepository) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.dashboardService = dashboardService;
        this.recommendationService = recommendationService;
        this.approvalRepository = approvalRepository;
        this.supplierRepository = supplierRepository;
    }

    public AssistantAnswer ask(AssistantQuestion request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }
        if (request.question().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must be 1000 characters or fewer");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI provider is not configured. Set AI_API_KEY to enable assistant answers.");
        }

        String context = buildContext();
        ProviderRequest payload = new ProviderRequest(model, List.of(
                new ProviderMessage("system", "You are the read-only RetailOps manager assistant. "
                        + "Answer using only the supplied current application data. If the data does not contain an answer, say so. "
                        + "Never claim an action was performed; managers must use the approval workflow."),
                new ProviderMessage("user", "Question: " + request.question() + "\n\nCurrent application data (JSON):\n" + context)), 0.2);

        try {
            ProviderResponse response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(ProviderResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null
                    || response.choices().getFirst().message().content() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider returned no answer");
            }
            return new AssistantAnswer(response.choices().getFirst().message().content());
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider request failed");
        }
    }

    private String buildContext() {
        List<Map<String, Object>> pending = approvalRepository.findByStatusIgnoreCase("PENDING").stream()
                .map(approval -> Map.<String, Object>of(
                        "actionId", approval.getActionId(),
                        "storeId", approval.getStoreId(),
                        "productId", approval.getProductId(),
                        "action", approval.getActionType(),
                        "recommendedQuantity", approval.getRecommendedQuantity(),
                        "reason", approval.getReason(),
                        "status", approval.getStatus()))
                .toList();
        List<Map<String, Object>> supplierTerms = supplierRepository.findAll().stream()
                .map(supplier -> Map.<String, Object>of(
                        "productId", supplier.getProductId(),
                        "supplierName", supplier.getSupplierName(),
                        "leadTimeDays", supplier.getLeadTimeDays(),
                        "minimumOrderQuantity", supplier.getMinimumOrderQuantity(),
                        "unitCost", supplier.getUnitCost()))
                .toList();
        Map<String, Object> context = Map.of(
                "dashboard", dashboardService.getOverview(),
                "recommendations", recommendationService.getReorderRecommendations(),
                "pendingApprovals", pending,
                "supplierTerms", supplierTerms);
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JacksonException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to prepare current retail data");
        }
    }

    private record ProviderRequest(String model, List<ProviderMessage> messages, double temperature) {
    }

    private record ProviderMessage(String role, String content) {
    }

    private record ProviderResponse(List<ProviderChoice> choices) {
    }

    private record ProviderChoice(ProviderMessage message) {
    }
}