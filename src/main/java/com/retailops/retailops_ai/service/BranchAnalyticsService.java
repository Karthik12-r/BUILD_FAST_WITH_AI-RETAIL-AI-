package com.retailops.retailops_ai.service;

import com.retailops.retailops_ai.dto.BranchOverview;
import com.retailops.retailops_ai.dto.BranchSummary;
import com.retailops.retailops_ai.entity.ManagerApproval;
import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.entity.Supplier;
import com.retailops.retailops_ai.repository.ManagerApprovalRepository;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import com.retailops.retailops_ai.repository.SupplierRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BranchAnalyticsService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;
    private final SalesDataRepository salesDataRepository;
    private final SupplierRepository supplierRepository;
    private final ManagerApprovalRepository approvalRepository;
    private final ProductNameCatalog productNameCatalog;
    private final List<BranchDefinition> branches;

    public BranchAnalyticsService(
            JdbcTemplate jdbcTemplate,
            SalesDataRepository salesDataRepository,
            SupplierRepository supplierRepository,
            ManagerApprovalRepository approvalRepository,
            ProductNameCatalog productNameCatalog) {
        this.jdbcTemplate = jdbcTemplate;
        this.salesDataRepository = salesDataRepository;
        this.supplierRepository = supplierRepository;
        this.approvalRepository = approvalRepository;
        this.productNameCatalog = productNameCatalog;
        this.branches = loadBranches();
    }

    public List<BranchSummary> getBranches() {
        Map<String, BranchLatestMetrics> latestByBranch = latestMetricsForBranches();
        List<SalesData> latestSales = salesDataRepository.findLatestByStoreAndProduct();
        Map<String, Supplier> suppliersByProduct = suppliersByProduct();
        List<ManagerApproval> approvals = approvalRepository.findAll();
        return branches.stream()
            .map(branch -> summarize(
                branch,
                latestByBranch.get(branch.id()),
                inventoryMetrics(branch, latestSales, suppliersByProduct),
                pendingApprovals(branch, approvals).size()))
            .toList();
    }

    public BranchOverview getOverview(String branchId, String period) {
        BranchDefinition branch = findBranch(branchId);
        int days = periodDays(period);
        BranchLatestMetrics latest = latestMetricsForBranches().get(branch.id());
        LocalDate dataAsOf = latest == null ? null : latest.dataAsOf();
        if (dataAsOf == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No sales data is available for this branch");
        }

        LocalDate periodStart = dataAsOf.minusDays(days - 1L);
        LocalDate previousEnd = periodStart.minusDays(1);
        LocalDate previousStart = previousEnd.minusDays(days - 1L);
        PeriodMetrics current = periodMetrics(branch, periodStart, dataAsOf);
        PeriodMetrics previous = periodMetrics(branch, previousStart, previousEnd);
        List<SalesData> latestSales = salesDataRepository.findLatestByStoreAndProduct();
        Map<String, Supplier> suppliersByProduct = suppliersByProduct();
        List<ManagerApproval> approvals = approvalRepository.findAll();
        InventoryMetrics inventory = inventoryMetrics(branch, latestSales, suppliersByProduct);
        List<ManagerApproval> pending = pendingApprovals(branch, approvals);
        long pendingApprovals = pending.size();
        BranchSummary summary = summarize(branch, latest, inventory, pendingApprovals);

        return new BranchOverview(
                summary,
                period.toUpperCase(Locale.ROOT),
                periodStart,
                dataAsOf,
                current.revenue(),
                previous.revenue(),
                percentageChange(current.revenue(), previous.revenue()),
                current.unitsSold(),
                previous.unitsSold(),
                current.recordCount() == 0 ? BigDecimal.ZERO : current.revenue()
                        .divide(BigDecimal.valueOf(current.recordCount()), 2, RoundingMode.HALF_UP),
                inventory.inventoryValue(),
                inventory.healthyItems(),
                inventory.lowStockItems(),
                inventory.outOfStockItems(),
                pendingApprovals,
                previous.revenue(),
                percentageOf(current.revenue(), previous.revenue()),
                revenueTrend(branch, periodStart, dataAsOf, previousStart, days),
                topProducts(branch, periodStart, dataAsOf),
                recentOperations(branch, dataAsOf, approvals),
                insight(current.revenue(), previous.revenue(), period, inventory, pendingApprovals));
    }

    private BranchSummary summarize(
            BranchDefinition branch,
            BranchLatestMetrics latest,
            InventoryMetrics inventory,
            long pendingCount) {
        return new BranchSummary(
                branch.id(), branch.name(), branch.storeIds(), latest == null ? null : latest.dataAsOf(),
                latest == null ? BigDecimal.ZERO : latest.revenue(),
                latest == null ? 0 : latest.unitsSold(), inventory.healthyItems(),
                inventory.lowStockItems(), inventory.outOfStockItems(), pendingCount,
                branchStatus(inventory, pendingCount));
    }

    private Map<String, BranchLatestMetrics> latestMetricsForBranches() {
        List<String> branchRows = branches.stream()
                .flatMap(branch -> branch.storeIds().stream().map(storeId -> "select ? as branch_id, ? as store_id"))
                .toList();
        String sql = """
                with branch_stores as (
                    %s
                ), latest_dates as (
                    select branch_stores.branch_id, max(sales.`date`) as data_as_of
                    from branch_stores
                    left join sales_data sales on sales.store_id = branch_stores.store_id
                    group by branch_stores.branch_id
                )
                select latest_dates.branch_id, latest_dates.data_as_of,
                       coalesce(sum(cast(sales.units_sold as decimal(18, 2)) * cast(sales.price as decimal(18, 2))), 0) as revenue,
                       coalesce(sum(sales.units_sold), 0) as units_sold
                from latest_dates
                left join branch_stores on branch_stores.branch_id = latest_dates.branch_id
                left join sales_data sales
                       on sales.store_id = branch_stores.store_id
                      and sales.`date` = latest_dates.data_as_of
                group by latest_dates.branch_id, latest_dates.data_as_of
                """.formatted(String.join(" union all ", branchRows));
        Object[] args = branches.stream()
                .flatMap(branch -> branch.storeIds().stream().flatMap(storeId -> java.util.stream.Stream.of(branch.id(), storeId)))
                .toArray();
        return jdbcTemplate.query(sql, resultSet -> {
            Map<String, BranchLatestMetrics> metrics = new HashMap<>();
            while (resultSet.next()) {
                Date sqlDate = resultSet.getDate("data_as_of");
                metrics.put(resultSet.getString("branch_id"), new BranchLatestMetrics(
                        sqlDate == null ? null : sqlDate.toLocalDate(),
                        zeroIfNull(resultSet.getBigDecimal("revenue")),
                        resultSet.getLong("units_sold")));
            }
            return metrics;
        }, args);
    }

    private PeriodMetrics periodMetrics(BranchDefinition branch, LocalDate start, LocalDate end) {
        String sql = """
                select coalesce(sum(cast(units_sold as decimal(18, 2)) * cast(price as decimal(18, 2))), 0) as revenue,
                       coalesce(sum(units_sold), 0) as units_sold,
                       count(*) as record_count
                from sales_data
                where store_id in (%s) and `date` between ? and ?
                """.formatted(placeholders(branch));
        Object[] args = queryArguments(branch, start, end);
        return jdbcTemplate.queryForObject(sql, (resultSet, rowNumber) -> new PeriodMetrics(
                zeroIfNull(resultSet.getBigDecimal("revenue")),
                resultSet.getLong("units_sold"),
                resultSet.getLong("record_count")), args);
    }

        private InventoryMetrics inventoryMetrics(
            BranchDefinition branch,
            List<SalesData> latestSales,
            Map<String, Supplier> suppliersByProduct) {
        Set<String> storeIds = Set.copyOf(branch.storeIds());
        int healthy = 0;
        int low = 0;
        int out = 0;
        BigDecimal inventoryValue = BigDecimal.ZERO;

        for (SalesData sale : latestSales) {
            if (!storeIds.contains(sale.getStoreId()) || sale.getInventoryLevel() == null) continue;
            Supplier supplier = suppliersByProduct.get(sale.getProductId());
            BigDecimal unitCost = supplier == null || supplier.getUnitCost() == null
                    ? BigDecimal.ZERO
                    : supplier.getUnitCost();
            inventoryValue = inventoryValue.add(unitCost.multiply(BigDecimal.valueOf(sale.getInventoryLevel())));

            if (sale.getInventoryLevel() == 0) {
                out++;
            } else if (sale.getDemand() != null && supplier != null && supplier.getLeadTimeDays() != null
                    && sale.getInventoryLevel() < (long) sale.getDemand() * supplier.getLeadTimeDays()) {
                low++;
            } else {
                healthy++;
            }
        }
        return new InventoryMetrics(healthy, low, out, inventoryValue);
    }

    private List<BranchOverview.RevenuePoint> revenueTrend(
            BranchDefinition branch,
            LocalDate currentStart,
            LocalDate currentEnd,
            LocalDate previousStart,
            int days) {
        LocalDate previousEnd = previousStart.plusDays(days - 1L);
        String sql = """
                select `date` as sale_date,
                       coalesce(sum(cast(units_sold as decimal(18, 2)) * cast(price as decimal(18, 2))), 0) as revenue
                from sales_data
                where store_id in (%s) and `date` between ? and ?
                group by `date`
                """.formatted(placeholders(branch));
        Map<LocalDate, BigDecimal> daily = new HashMap<>();
        jdbcTemplate.query(sql, resultSet -> {
            daily.put(resultSet.getDate("sale_date").toLocalDate(),
                zeroIfNull(resultSet.getBigDecimal("revenue")));
        }, queryArguments(branch, previousStart, currentEnd));

        List<BranchOverview.RevenuePoint> points = new ArrayList<>(days);
        for (int index = 0; index < days; index++) {
            LocalDate date = currentStart.plusDays(index);
            LocalDate previousDate = previousStart.plusDays(index);
            points.add(new BranchOverview.RevenuePoint(
                    date,
                    daily.getOrDefault(date, BigDecimal.ZERO),
                    daily.getOrDefault(previousDate, BigDecimal.ZERO)));
        }
        return points;
    }

    private List<BranchOverview.ProductPerformance> topProducts(
            BranchDefinition branch,
            LocalDate start,
            LocalDate end) {
        String sql = """
                select product_id, max(category) as category, coalesce(sum(units_sold), 0) as units_sold,
                       coalesce(sum(cast(units_sold as decimal(18, 2)) * cast(price as decimal(18, 2))), 0) as revenue
                from sales_data
                where store_id in (%s) and `date` between ? and ?
                group by product_id
                order by units_sold desc
                limit 5
                """.formatted(placeholders(branch));
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> {
            String productId = resultSet.getString("product_id");
            String category = resultSet.getString("category");
            return new BranchOverview.ProductPerformance(
                    productId,
                    productNameCatalog.displayName(productId, category),
                    category,
                    resultSet.getLong("units_sold"),
                    zeroIfNull(resultSet.getBigDecimal("revenue")));
        }, queryArguments(branch, start, end));
    }

    private List<BranchOverview.RecentOperation> recentOperations(
            BranchDefinition branch,
            LocalDate dataAsOf,
            List<ManagerApproval> approvals) {
        Set<String> storeIds = Set.copyOf(branch.storeIds());
        List<BranchOverview.RecentOperation> operations = approvals.stream()
                .filter(approval -> storeIds.contains(approval.getStoreId()))
                .sorted(Comparator.comparing(this::operationDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(this::toOperation)
                .collect(Collectors.toCollection(ArrayList::new));
        operations.add(new BranchOverview.RecentOperation(
                "SALES DATA", "Sales records available through " + dataAsOf, "RECORDED", dataAsOf.toString()));
        return operations.stream().limit(7).toList();
    }

    private BranchOverview.RecentOperation toOperation(ManagerApproval approval) {
        String status = approval.getStatus() == null ? "UNKNOWN" : approval.getStatus().toUpperCase(Locale.ROOT);
        String action = switch (status) {
            case "APPROVED" -> "Reorder approved";
            case "REJECTED" -> "Reorder rejected";
            default -> "Reorder awaiting manager review";
        };
        LocalDateTime timestamp = operationDate(approval);
        return new BranchOverview.RecentOperation(
                "APPROVAL",
                action + " for " + approval.getProductId(),
                status,
                timestamp == null ? "Date unavailable" : DATE_TIME_FORMAT.format(timestamp));
    }

    private LocalDateTime operationDate(ManagerApproval approval) {
        if (approval.getApprovalTimestamp() != null) return approval.getApprovalTimestamp();
        return approval.getApprovalDate() == null ? null : approval.getApprovalDate().atTime(LocalTime.MIDNIGHT);
    }

    private List<ManagerApproval> pendingApprovals(BranchDefinition branch, List<ManagerApproval> approvals) {
        Set<String> storeIds = Set.copyOf(branch.storeIds());
        return approvals.stream()
                .filter(approval -> "PENDING".equalsIgnoreCase(approval.getStatus()))
                .filter(approval -> storeIds.contains(approval.getStoreId()))
                .toList();
    }

    private Map<String, Supplier> suppliersByProduct() {
        return supplierRepository.findAll().stream()
                .collect(Collectors.toMap(Supplier::getProductId, supplier -> supplier, (first, ignored) -> first));
    }

    private String insight(
            BigDecimal currentRevenue,
            BigDecimal previousRevenue,
            String period,
            InventoryMetrics inventory,
            long pendingApprovals) {
        BigDecimal change = percentageChange(currentRevenue, previousRevenue);
        String trend = change == null
                ? "Sales baseline is not available for comparison."
                : "Revenue is " + (change.signum() >= 0 ? "up " : "down ")
                        + change.abs().toPlainString() + "% versus the previous " + period + " period.";
        return trend + " " + inventory.outOfStockItems() + " products are out of stock, "
                + inventory.lowStockItems() + " are below lead-time coverage, and "
                + pendingApprovals + " manager approvals are pending.";
    }

    private String branchStatus(InventoryMetrics inventory, long pendingApprovals) {
        if (inventory.outOfStockItems() > 0) return "Critical";
        if (inventory.lowStockItems() > 0 || pendingApprovals > 0) return "Needs attention";
        return "Operating normally";
    }

    private BigDecimal percentageChange(BigDecimal current, BigDecimal previous) {
        if (previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal percentageOf(BigDecimal current, BigDecimal baseline) {
        if (baseline.signum() == 0) return null;
        return current.multiply(BigDecimal.valueOf(100)).divide(baseline, 1, RoundingMode.HALF_UP);
    }

    private int periodDays(String period) {
        return switch (period.toUpperCase(Locale.ROOT)) {
            case "7D" -> 7;
            case "30D" -> 30;
            case "90D" -> 90;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be 7D, 30D, or 90D");
        };
    }

    private BranchDefinition findBranch(String branchId) {
        return branches.stream().filter(branch -> branch.id().equalsIgnoreCase(branchId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    private String placeholders(BranchDefinition branch) {
        return String.join(",", java.util.Collections.nCopies(branch.storeIds().size(), "?"));
    }

    private Object[] queryArguments(BranchDefinition branch, LocalDate start, LocalDate end) {
        List<Object> args = new ArrayList<>(branch.storeIds());
        args.add(Date.valueOf(start));
        args.add(Date.valueOf(end));
        return args.toArray();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<BranchDefinition> loadBranches() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("branches.csv").getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .skip(1)
                    .map(line -> line.split(",", 3))
                    .filter(parts -> parts.length == 3)
                    .map(parts -> new BranchDefinition(
                            parts[0].trim(),
                            parts[1].trim(),
                            List.of(parts[2].trim().split("\\|"))))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load branches.csv", exception);
        }
    }

    private record BranchDefinition(String id, String name, List<String> storeIds) {
    }

    private record BranchLatestMetrics(LocalDate dataAsOf, BigDecimal revenue, long unitsSold) {
    }

    private record PeriodMetrics(BigDecimal revenue, long unitsSold, long recordCount) {
    }

    private record InventoryMetrics(int healthyItems, int lowStockItems, int outOfStockItems, BigDecimal inventoryValue) {
    }
}