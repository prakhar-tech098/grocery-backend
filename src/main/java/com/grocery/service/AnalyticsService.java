package com.grocery.service;

import com.grocery.dto.AnalyticsDto;
import com.grocery.entity.Product;
import com.grocery.repository.InventoryRepository;
import com.grocery.repository.OrderItemRepository;
import com.grocery.repository.OrderRepository;
import com.grocery.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @Autowired private OrderItemRepository orderItemRepo;
    @Autowired private OrderRepository     orderRepo;
    @Autowired private ProductRepository   productRepo;
    @Autowired private InventoryRepository inventoryRepo;

    // ═══════════════════════════════════════════════════════════════════════════
    // 1. SALES ANALYTICS DASHBOARD
    // ═══════════════════════════════════════════════════════════════════════════

    public AnalyticsDto.SalesReport getSalesReport(int days) {
        LocalDateTime since = LocalDate.now().minusDays(days).atStartOfDay();

        // ── Raw data from DB ──────────────────────────────────────────────────
        List<Object[]> dailyRows    = orderItemRepo.findDailySales(since);
        List<Object[]> topProdRows  = orderItemRepo.findTopProductsByRevenue(since);
        List<Object[]> categoryRows = orderItemRepo.findSalesByCategory(since);

        // ── Daily sales ───────────────────────────────────────────────────────
        List<AnalyticsDto.DailySales> dailySales = dailyRows.stream()
            .map(r -> new AnalyticsDto.DailySales(
                r[0].toString(),
                toBigDecimal(r[1]),
                toLong(r[2])
            ))
            .collect(Collectors.toList());

        // Fill in missing days with zero so the chart has no gaps
        dailySales = fillMissingDays(dailySales, days);

        // ── Top products (limit 10) ───────────────────────────────────────────
        List<AnalyticsDto.TopProduct> topProducts = topProdRows.stream()
            .limit(10)
            .map(r -> new AnalyticsDto.TopProduct(
                toLong(r[0]),
                (String) r[1],
                toBigDecimal(r[2]),
                toLong(r[3])
            ))
            .collect(Collectors.toList());

        // ── Sales by category ─────────────────────────────────────────────────
        BigDecimal catTotal = categoryRows.stream()
            .map(r -> toBigDecimal(r[1]))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalyticsDto.CategorySales> salesByCategory = categoryRows.stream()
            .map(r -> {
                BigDecimal rev = toBigDecimal(r[1]);
                double pct = catTotal.compareTo(BigDecimal.ZERO) > 0
                    ? rev.divide(catTotal, 4, RoundingMode.HALF_UP)
                          .multiply(BigDecimal.valueOf(100))
                          .doubleValue()
                    : 0.0;
                return new AnalyticsDto.CategorySales((String) r[0], rev, pct);
            })
            .collect(Collectors.toList());

        // ── Summary totals ────────────────────────────────────────────────────
        BigDecimal totalRevenue = dailySales.stream()
            .map(AnalyticsDto.DailySales::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long totalUnitsSold = topProducts.stream()
            .mapToLong(AnalyticsDto.TopProduct::getUnitsSold)
            .sum();

        Long totalOrders = orderRepo.countOrdersBetween(since, LocalDateTime.now());

        BigDecimal avgOrderValue = (totalOrders != null && totalOrders > 0)
            ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return AnalyticsDto.SalesReport.builder()
            .days(days)
            .totalRevenue(totalRevenue)
            .totalOrders(totalOrders != null ? totalOrders : 0L)
            .totalUnitsSold(totalUnitsSold)
            .avgOrderValue(avgOrderValue)
            .dailySales(dailySales)
            .topProducts(topProducts)
            .salesByCategory(salesByCategory)
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2. DEMAND PREDICTION + 3. SMART RESTOCK ALERTS
    // ═══════════════════════════════════════════════════════════════════════════

    public List<AnalyticsDto.DemandForecast> getDemandForecasts() {
        LocalDateTime since30 = LocalDate.now().minusDays(30).atStartOfDay();

        // Units sold per product in last 30 days
        List<Object[]> soldRows = orderItemRepo.findUnitsSoldPerProduct(since30);

        Map<Long, Long> soldMap = soldRows.stream()
            .collect(Collectors.toMap(
                r -> toLong(r[0]),
                r -> toLong(r[1])
            ));

        List<Product> allProducts = productRepo.findAll();

        return allProducts.stream()
            .map(product -> {
                long soldLast30 = soldMap.getOrDefault(product.getId(), 0L);
                double avgDaily  = soldLast30 / 30.0;
                int    stock     = product.getStockQuantity();

                // Days until stockout — cap at 999 when no sales
                int daysUntilStockout = (avgDaily > 0)
                    ? (int) Math.min(999, Math.floor(stock / avgDaily))
                    : 999;

                // Suggested restock = 30 days of supply (minus what's already in stock, min 0)
                int suggested = (int) Math.max(0, Math.ceil(avgDaily * 30) - stock);

                // Urgency bands
                String urgency;
                if (daysUntilStockout <= 3)        urgency = "CRITICAL";
                else if (daysUntilStockout <= 7)   urgency = "WARNING";
                else if (daysUntilStockout <= 14)  urgency = "LOW";
                else                               urgency = "OK";

                return AnalyticsDto.DemandForecast.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .currentStock(stock)
                    .unitsSoldLast30Days(soldLast30)
                    .avgDailySales(round2(avgDaily))
                    .daysUntilStockout(daysUntilStockout)
                    .suggestedRestockQty(suggested)
                    .urgency(urgency)
                    .build();
            })
            // Sort: CRITICAL first, then by daysUntilStockout ascending
            .sorted(Comparator.comparingInt(f -> urgencyOrder(f.getUrgency())))
            .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4. PRODUCT RECOMMENDATIONS (Market Basket)
    // ═══════════════════════════════════════════════════════════════════════════

    public List<AnalyticsDto.Recommendation> getRecommendations(Long productId) {
        List<Object[]> rows = orderItemRepo.findFrequentlyBoughtWith(productId);

        // Count how many orders contain the source product (for confidence calc)
        long sourceOrderCount = orderItemRepo.findFrequentlyBoughtWith(productId)
            .stream().mapToLong(r -> toLong(r[3])).sum();

        return rows.stream()
            .limit(5)
            .map(r -> {
                long coCount = toLong(r[3]);
                double confidence = sourceOrderCount > 0
                    ? round2((double) coCount / sourceOrderCount * 100)
                    : 0.0;
                return new AnalyticsDto.Recommendation(
                    toLong(r[0]),
                    (String) r[1],
                    toBigDecimal(r[2]),
                    coCount,
                    confidence
                );
            })
            .collect(Collectors.toList());
    }

    // Get global top recommendations (used when no product is selected)
    public Map<Long, List<AnalyticsDto.Recommendation>> getAllRecommendations() {
        List<Object[]> pairs = orderItemRepo.findCoPurchasePairs();

        // Build map: productId -> list of (partnerId, coCount)
        Map<Long, Map<Long, Long>> matrix = new HashMap<>();

        for (Object[] row : pairs) {
            long p1 = toLong(row[0]);
            long p2 = toLong(row[1]);
            long ct = toLong(row[2]);
            matrix.computeIfAbsent(p1, k -> new HashMap<>()).put(p2, ct);
            matrix.computeIfAbsent(p2, k -> new HashMap<>()).put(p1, ct);
        }

        // Build product name cache
        Map<Long, String> names = productRepo.findAll().stream()
            .collect(Collectors.toMap(Product::getId, Product::getName));
        Map<Long, BigDecimal> prices = productRepo.findAll().stream()
            .collect(Collectors.toMap(Product::getId, Product::getPrice));

        Map<Long, List<AnalyticsDto.Recommendation>> result = new HashMap<>();

        for (Map.Entry<Long, Map<Long, Long>> entry : matrix.entrySet()) {
            Long sourceId = entry.getKey();
            long sourceTotal = entry.getValue().values().stream().mapToLong(v -> v).sum();

            List<AnalyticsDto.Recommendation> recs = entry.getValue().entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(3)
                .map(e -> new AnalyticsDto.Recommendation(
                    e.getKey(),
                    names.getOrDefault(e.getKey(), "Unknown"),
                    prices.getOrDefault(e.getKey(), BigDecimal.ZERO),
                    e.getValue(),
                    sourceTotal > 0 ? round2((double) e.getValue() / sourceTotal * 100) : 0.0
                ))
                .collect(Collectors.toList());

            result.put(sourceId, recs);
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private int urgencyOrder(String urgency) {
        return switch (urgency) {
            case "CRITICAL" -> 0;
            case "WARNING"  -> 1;
            case "LOW"      -> 2;
            default         -> 3;
        };
    }

    /** Fill gaps in daily sales so the chart never has missing dates */
    private List<AnalyticsDto.DailySales> fillMissingDays(
            List<AnalyticsDto.DailySales> existing, int days) {

        Map<String, AnalyticsDto.DailySales> byDate = existing.stream()
            .collect(Collectors.toMap(AnalyticsDto.DailySales::getDate, d -> d));

        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;
        List<AnalyticsDto.DailySales> filled = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            String date = LocalDate.now().minusDays(i).format(fmt);
            filled.add(byDate.getOrDefault(date,
                new AnalyticsDto.DailySales(date, BigDecimal.ZERO, 0L)));
        }
        return filled;
    }
}
