package com.grocery.controller;

import com.grocery.dto.AnalyticsDto;
import com.grocery.dto.ApiResponse;
import com.grocery.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    // ─────────────────────────────────────────────────────────────────────────
    // 1. SALES ANALYTICS DASHBOARD
    // GET /api/analytics/sales?days=30
    // days param: 7, 30, or 90  (default: 30)
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<AnalyticsDto.SalesReport>> getSalesReport(
            @RequestParam(defaultValue = "30") int days) {

        // Clamp to valid options
        if (days != 7 && days != 90) days = 30;

        AnalyticsDto.SalesReport report = analyticsService.getSalesReport(days);
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2 + 3. DEMAND PREDICTION + SMART RESTOCK ALERTS
    // GET /api/analytics/demand
    // Returns all products with: days-to-stockout, avg daily sales, urgency,
    // and suggested restock quantity
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/demand")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.DemandForecast>>> getDemandForecasts() {
        List<AnalyticsDto.DemandForecast> forecasts = analyticsService.getDemandForecasts();
        return ResponseEntity.ok(ApiResponse.ok(forecasts));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. PRODUCT RECOMMENDATIONS
    // GET /api/analytics/recommendations/{productId}
    // Returns top 5 products frequently bought with the given product
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/recommendations/{productId}")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.Recommendation>>> getRecommendations(
            @PathVariable Long productId) {

        List<AnalyticsDto.Recommendation> recs = analyticsService.getRecommendations(productId);
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/analytics/recommendations
    // Returns the full co-purchase map (productId -> top 3 recommendations)
    // Used to pre-load recommendations for the New Order modal
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/recommendations")
    public ResponseEntity<ApiResponse<Map<Long, List<AnalyticsDto.Recommendation>>>> getAllRecommendations() {
        Map<Long, List<AnalyticsDto.Recommendation>> recs = analyticsService.getAllRecommendations();
        return ResponseEntity.ok(ApiResponse.ok(recs));
    }
}
