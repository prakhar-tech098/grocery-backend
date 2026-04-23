package com.grocery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class AnalyticsDto {

    // ─── Daily sales data point (for line chart) ──────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DailySales {
        private String date;          // "2024-03-15"
        private BigDecimal revenue;
        private Long unitsSold;
    }

    // ─── Top product by revenue (for bar chart) ───────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TopProduct {
        private Long   productId;
        private String productName;
        private BigDecimal revenue;
        private Long   unitsSold;
    }

    // ─── Category revenue slice (for pie chart) ───────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CategorySales {
        private String category;
        private BigDecimal revenue;
        private Double percentage;    // filled in by service
    }

    // ─── Full sales analytics response ────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SalesReport {
        private int         days;             // period requested (7, 30, 90)
        private BigDecimal  totalRevenue;
        private Long        totalOrders;
        private Long        totalUnitsSold;
        private BigDecimal  avgOrderValue;
        private List<DailySales>    dailySales;
        private List<TopProduct>    topProducts;
        private List<CategorySales> salesByCategory;
    }

    // ─── Demand prediction for one product ────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DemandForecast {
        private Long   productId;
        private String productName;
        private Integer currentStock;
        private Long   unitsSoldLast30Days;
        private Double avgDailySales;         // units/day
        private Integer daysUntilStockout;    // currentStock / avgDailySales
        private Integer suggestedRestockQty;  // 30 days of supply
        private String  urgency;              // CRITICAL, WARNING, OK
    }

    // ─── Product recommendation (frequently bought together) ──────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Recommendation {
        private Long       productId;
        private String     productName;
        private BigDecimal price;
        private Long       coPurchaseCount;   // how often bought together
        private Double     confidence;        // coCount / totalOrdersWithSource
    }
}
