package com.retailops.retailops_ai;

import com.retailops.retailops_ai.dto.ApprovalDecisionRequest;
import com.retailops.retailops_ai.dto.AssistantQuestion;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import com.retailops.retailops_ai.service.ApprovalService;
import com.retailops.retailops_ai.service.DashboardAnalyticsService;
import com.retailops.retailops_ai.service.RecommendationApprovalService;
import com.retailops.retailops_ai.service.RecommendationService;
import com.retailops.retailops_ai.service.RetailAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "retailops.ai.api-key=")
class RetailopsAiApplicationTests {
	@Autowired
	private SalesDataRepository salesDataRepository;

	@Autowired
	private SupplierRepository supplierRepository;

	@Autowired
	private ManagerApprovalRepository approvalRepository;

	@Autowired
	private RecommendationService recommendationService;

	@Autowired
	private RecommendationApprovalService recommendationApprovalService;

	@Autowired
	private ApprovalService approvalService;

	@Autowired
	private DashboardAnalyticsService dashboardAnalyticsService;

	@Autowired
	private RetailAssistantService assistantService;

	@Test
	void contextLoads() {
	}

	@Test
	void importedSalesSuppliersAndApprovalsAreReadable() {
		assertThat(salesDataRepository.findAll(PageRequest.of(0, 5))).hasSize(5);
		assertThat(supplierRepository.findAll()).hasSize(20);
		assertThat(approvalRepository.findAll()).isNotEmpty();
	}

	@Test
	void recommendationsUseLatestDataAndExcludePendingPairs() {
		var pendingPairs = approvalRepository.findByStatusIgnoreCase("PENDING").stream()
				.map(approval -> approval.getStoreId() + ":" + approval.getProductId())
				.toList();
		var recommendations = recommendationService.getReorderRecommendations();

		assertThat(recommendations).isNotEmpty();
		assertThat(recommendations).allSatisfy(recommendation -> {
			assertThat(recommendation.reason()).contains("days of demand coverage");
			assertThat(recommendation.recommendedQuantity()).isPositive();
			assertThat(pendingPairs).doesNotContain(recommendation.storeId() + ":" + recommendation.productId());
		});
	}

	@Test
	void dashboardMetricsComeFromImportedSalesAndOperationsData() {
		var overview = dashboardAnalyticsService.getOverview();

		assertThat(overview.totalRevenue()).isPositive();
		assertThat(overview.totalUnitsSold()).isPositive();
		assertThat(overview.supplierCount()).isEqualTo(20);
		assertThat(overview.topProducts()).hasSize(5);
		assertThat(overview.salesTrend()).isNotEmpty();
	}

	@Test
	void assistantExplainsThatProviderMustBeConfigured() {
		assertThatThrownBy(() -> assistantService.ask(new AssistantQuestion("Which products need reordering?")))
				.isInstanceOfSatisfying(ResponseStatusException.class, exception ->
						assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
	}

	@Test
	@Transactional
	void approvalGenerationIsIdempotentWithoutCommittingTestRows() {
		var created = recommendationApprovalService.createPendingApprovals();

		assertThat(created).isNotEmpty();
		assertThat(created).allSatisfy(approval -> {
			assertThat(approval.getActionId()).hasSize(30);
			assertThat(approval.getStatus()).isEqualTo("PENDING");
		});
		assertThat(recommendationApprovalService.createPendingApprovals()).isEmpty();
	}

	@Test
	@Transactional
	void approvalDecisionIsAuditedAndCannotBeRepeated() {
		ManagerApproval pending = approvalRepository.findByStatusIgnoreCase("PENDING").stream()
				.findFirst()
				.orElseThrow();
		ApprovalDecisionRequest request = new ApprovalDecisionRequest();
		request.setApprover("Integration Test Manager");
		request.setManagerComment("Approved in rollback-only integration test");

		ManagerApproval decided = approvalService.decide(pending.getActionId(), "APPROVED", request);

		assertThat(decided.getStatus()).isEqualTo("APPROVED");
		assertThat(decided.getApprover()).isEqualTo("Integration Test Manager");
		assertThat(decided.getApprovalTimestamp()).isNotNull();
		assertThatThrownBy(() -> approvalService.decide(pending.getActionId(), "REJECTED", request))
				.isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
	}

}
