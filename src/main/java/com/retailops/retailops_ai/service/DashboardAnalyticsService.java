package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.DashboardOverview;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class DashboardAnalyticsService {

    private static final String TOTALS_SQL = """
            select coalesce(sum(cast(units_sold as decimal(18, 2)) * cast(price as decimal(18, 2))), 0) as total_revenue,
                   coalesce(sum(units_sold), 0) as total_units_sold
            from sales_data
            """;

    private static final String TOP_PRODUCTS_SQL = """
            select product_id, sum(units_sold) as units_sold
            from sales_data
            group by product_id
            order by units_sold desc
            limit 5
            """;

    private static final String MONTHLY_SALES_SQL = """
            select month, revenue, units_sold
            from (
                select date_format(`date`, '%Y-%m') as month,
                       coalesce(sum(cast(units_sold as decimal(18, 2)) * cast(price as decimal(18, 2))), 0) as revenue,
                       coalesce(sum(units_sold), 0) as units_sold
                from sales_data
                group by date_format(`date`, '%Y-%m')
                order by month desc
                limit 12
            ) recent_months
            order by month asc
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RecommendationService recommendationService;
        private final ManagerApprovalRepository approvalRepository;
        private final SupplierRepository supplierRepository;

    public DashboardAnalyticsService(
            JdbcTemplate jdbcTemplate,
            RecommendationService recommendationService,
                        ManagerApprovalRepository approvalRepository,
                        SupplierRepository supplierRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.recommendationService = recommendationService;
        this.approvalRepository = approvalRepository;
        this.supplierRepository = supplierRepository;
    }

    public DashboardOverview getOverview() {
        Map<String, Object> totals = jdbcTemplate.queryForMap(TOTALS_SQL);
        BigDecimal revenue = toBigDecimal(totals.get("total_revenue"));
        long unitsSold = ((Number) totals.get("total_units_sold")).longValue();

        List<DashboardOverview.ProductSales> topProducts = jdbcTemplate.query(
                TOP_PRODUCTS_SQL,
                (resultSet, rowNumber) -> new DashboardOverview.ProductSales(
                        resultSet.getString("product_id"), resultSet.getLong("units_sold")));
        List<DashboardOverview.MonthlySales> salesTrend = jdbcTemplate.query(
                MONTHLY_SALES_SQL,
                (resultSet, rowNumber) -> new DashboardOverview.MonthlySales(
                        resultSet.getString("month"), resultSet.getBigDecimal("revenue"),
                        resultSet.getLong("units_sold")));

        return new DashboardOverview(
                revenue,
                unitsSold,
                recommendationService.getReorderRecommendations().size(),
                approvalRepository.countByStatusIgnoreCase("PENDING"),
                supplierRepository.count(),
                topProducts,
                salesTrend);
    }

    private BigDecimal toBigDecimal(Object value) {
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}