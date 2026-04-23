package com.grocery.controller;

import com.grocery.dto.ApiResponse;
import com.grocery.dto.DashboardDto;
import com.grocery.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * GET /api/dashboard/stats
     * Returns summary: total products, orders, today's sales, month's sales, low stock
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardStats()));
    }
}
