package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.ReorderRecommendation;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.entity.Supplier;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationService {

    private final SalesDataRepository salesDataRepository;
    private final SupplierRepository supplierRepository;
    private final ManagerApprovalRepository approvalRepository;

    public RecommendationService(
            SalesDataRepository salesDataRepository,
            SupplierRepository supplierRepository,
            ManagerApprovalRepository approvalRepository) {
        this.salesDataRepository = salesDataRepository;
        this.supplierRepository = supplierRepository;
        this.approvalRepository = approvalRepository;
    }

    public List<ReorderRecommendation> getReorderRecommendations() {
        Map<String, Supplier> suppliersByProduct = new HashMap<>();
        for (Supplier supplier : supplierRepository.findAll()) {
            suppliersByProduct.putIfAbsent(supplier.getProductId(), supplier);
        }

        Set<StoreProduct> pendingActions = new HashSet<>();
        for (ManagerApproval approval : approvalRepository.findByStatusIgnoreCase("PENDING")) {
            pendingActions.add(new StoreProduct(approval.getStoreId(), approval.getProductId()));
        }

        List<ReorderRecommendation> recommendations = new ArrayList<>();
        for (SalesData sale : salesDataRepository.findLatestByStoreAndProduct()) {
            Supplier supplier = suppliersByProduct.get(sale.getProductId());
            if (supplier == null || sale.getInventoryLevel() == null || sale.getDemand() == null
                    || supplier.getLeadTimeDays() == null || supplier.getMinimumOrderQuantity() == null) {
                continue;
            }

            StoreProduct key = new StoreProduct(sale.getStoreId(), sale.getProductId());
            if (pendingActions.contains(key)) {
                continue;
            }

            long leadTimeDemand = (long) sale.getDemand() * supplier.getLeadTimeDays();
            if (sale.getInventoryLevel() >= leadTimeDemand) {
                continue;
            }

            long shortage = leadTimeDemand - sale.getInventoryLevel();
            int recommendedQuantity = (int) Math.min(Integer.MAX_VALUE,
                    Math.max(shortage, supplier.getMinimumOrderQuantity()));
            String priority = sale.getInventoryLevel() == 0 || sale.getInventoryLevel() * 2L < leadTimeDemand
                    ? "HIGH"
                    : "MEDIUM";
            String reason = "Inventory of " + sale.getInventoryLevel() + " is below "
                    + supplier.getLeadTimeDays() + " days of demand coverage ("
                    + leadTimeDemand + "); supplier minimum order is "
                    + supplier.getMinimumOrderQuantity() + ".";

            recommendations.add(new ReorderRecommendation(
                    sale.getProductId(),
                    sale.getStoreId(),
                    "REORDER",
                    recommendedQuantity,
                    reason,
                    priority,
                    sale.getInventoryLevel(),
                    sale.getDemand(),
                    supplier.getLeadTimeDays(),
                    supplier.getMinimumOrderQuantity(),
                    sale.getDate()));
        }

        recommendations.sort(Comparator.comparing(ReorderRecommendation::priority)
                .thenComparing(ReorderRecommendation::storeId)
                .thenComparing(ReorderRecommendation::productId));
        return recommendations;
    }

    private record StoreProduct(String storeId, String productId) {
    }
}