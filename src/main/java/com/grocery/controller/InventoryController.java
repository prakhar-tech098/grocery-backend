package com.grocery.controller;

import com.grocery.dto.ApiResponse;
import com.grocery.dto.InventoryDto;
import com.grocery.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * GET /api/inventory
     * Returns all inventory records
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllInventory()));
    }

    /**
     * GET /api/inventory/low-stock
     * Returns items that have fallen below their threshold
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryDto.Response>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockItems()));
    }

    /**
     * GET /api/inventory/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getByProductId(productId)));
    }

    /**
     * PUT /api/inventory/product/{productId}  (ADMIN only)
     * Manually update stock quantity and/or threshold
     */
    @PutMapping("/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryDto.Response>> updateInventory(
            @PathVariable Long productId,
            @RequestBody InventoryDto.UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Inventory updated", inventoryService.updateInventory(productId, request)));
    }
}
