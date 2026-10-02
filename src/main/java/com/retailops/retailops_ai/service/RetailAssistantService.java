package com.retailops.retailops_ai.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.retailops.retailops_ai.dto.AssistantAnswer;
import com.retailops.retailops_ai.dto.AssistantQuestion;
import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.entity.Supplier;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final SalesDataRepository salesDataRepository;
    private final SupplierRepository supplierRepository;
    private final ProductNameCatalog productNameCatalog;

    public RetailAssistantService(
            @Value("${retailops.ai.api-url}") String apiUrl,
            @Value("${retailops.ai.api-key}") String apiKey,
            @Value("${retailops.ai.model}") String model,
            ObjectMapper objectMapper,
            DashboardAnalyticsService dashboardService,
            RecommendationService recommendationService,
            ManagerApprovalRepository approvalRepository,
            SalesDataRepository salesDataRepository,
            SupplierRepository supplierRepository,
            ProductNameCatalog productNameCatalog) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.dashboardService = dashboardService;
        this.recommendationService = recommendationService;
        this.approvalRepository = approvalRepository;
        this.salesDataRepository = salesDataRepository;
        this.supplierRepository = supplierRepository;
        this.productNameCatalog = productNameCatalog;
    }

    public AssistantAnswer ask(AssistantQuestion request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question is required");
        }
        if (request.question().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "question must be 1000 characters or fewer");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return answerFromApplicationData(request.question());
        }

        String context = buildContext();
        ProviderRequest payload = new ProviderRequest(model, List.of(
            new ProviderMessage("system", "You are RetailOps AI, a helpful general-purpose conversational assistant. "
                + "Answer general knowledge, writing, reasoning, and everyday questions naturally and directly. "
                + "For questions about this RetailOps installation, use the supplied current application data and say when it is missing or stale. "
                + "Never invent company-specific facts, reveal credentials or private supplier contact details, "
                + "or claim that an action was performed; managers must use the approval workflow."),
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
                return answerFromApplicationData(request.question());
            }
            return new AssistantAnswer(response.choices().getFirst().message().content());
        } catch (RestClientException exception) {
            return answerFromApplicationData(request.question());
        }
    }

    private AssistantAnswer answerFromApplicationData(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        var dashboard = dashboardService.getOverview();
        long pendingCount = approvalRepository.countByStatusIgnoreCase("PENDING");
        String answer;

        Matcher productMatch = Pattern.compile("\\bP\\d{4}\\b", Pattern.CASE_INSENSITIVE).matcher(question);
        if (normalized.contains("supplier") && productMatch.find()) {
            String productId = productMatch.group().toUpperCase(Locale.ROOT);
            Supplier supplier = supplierRepository.findAllByProductId(productId).stream().findFirst().orElse(null);
            answer = supplier == null
                    ? "I couldn't find a supplier record for " + productId + "."
                    : supplier.getSupplierName() + " supplies " + productId + ". Lead time: "
                            + supplier.getLeadTimeDays() + " days; minimum order: "
                            + supplier.getMinimumOrderQuantity() + " units.";
        } else if (normalized.contains("stock") || normalized.contains("inventory")) {
            answer = stockInsight();
        } else if (normalized.contains("approval") || normalized.contains("pending")) {
            answer = "There are " + pendingCount + " manager approvals currently pending. "
                    + "Review them in the Approvals workspace; I can't approve or reject actions for you.";
        } else if (normalized.contains("supplier")) {
            answer = "The supplier directory contains " + dashboard.supplierCount()
                    + " supplier records. Ask about a product ID, such as P0018, to see its supplier terms.";
        } else {
            if (isGeneralQuestion(normalized)) {
                answer = "I can help with the connected RetailOps data. Try asking about sales, inventory, suppliers, or approvals.";
            } else {
                answer = "Current RetailOps snapshot: recorded revenue " + currency(dashboard.totalRevenue())
                        + " across " + NumberFormat.getIntegerInstance(Locale.US).format(dashboard.totalUnitsSold())
                        + " units sold. There are " + pendingCount + " pending manager approvals and "
                        + dashboard.reorderAlerts() + " reorder alerts. These figures use the imported data.";
            }
        }
        return new AssistantAnswer(answer);
    }

    private String stockInsight() {
        List<SalesData> latestSales = salesDataRepository.findLatestByStoreAndProduct();
        Map<String, Supplier> suppliersByProduct = supplierRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Supplier::getProductId, supplier -> supplier, (first, ignored) -> first));
        List<SalesData> outOfStock = latestSales.stream()
                .filter(sale -> sale.getInventoryLevel() != null && sale.getInventoryLevel() == 0)
                .toList();
        long lowStock = latestSales.stream().filter(sale -> {
            Supplier supplier = suppliersByProduct.get(sale.getProductId());
            return sale.getInventoryLevel() != null && sale.getInventoryLevel() > 0
                    && sale.getDemand() != null && supplier != null && supplier.getLeadTimeDays() != null
                    && sale.getInventoryLevel() < (long) sale.getDemand() * supplier.getLeadTimeDays();
        }).count();

        String outOfStockProducts = outOfStock.stream().limit(5)
                .map(sale -> productNameCatalog.displayName(sale.getProductId(), sale.getCategory())
                        + " at " + sale.getStoreId())
                .collect(java.util.stream.Collectors.joining(", "));
        String detail = outOfStock.isEmpty()
                ? "No products are currently recorded at zero stock."
                : "Out of stock: " + outOfStockProducts
                        + (outOfStock.size() > 5 ? ", and " + (outOfStock.size() - 5) + " more." : ".");
        return detail + " " + lowStock + " more products are below supplier lead-time coverage.";
    }

    private String currency(java.math.BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }

    private boolean isGeneralQuestion(String question) {
        return !question.contains("retail") && !question.contains("sales") && !question.contains("revenue")
                && !question.contains("product") && !question.contains("store") && !question.contains("branch")
                && !question.contains("supplier") && !question.contains("stock") && !question.contains("inventory")
                && !question.contains("approval") && !question.contains("reorder") && !question.contains("units");
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
