package com.grocery.service;

import com.grocery.dto.InventoryDto;
import com.grocery.entity.Inventory;
import com.grocery.exception.ResourceNotFoundException;
import com.grocery.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventoryService {

    @Autowired private InventoryRepository inventoryRepository;

    // ─── Get All Inventory ────────────────────────────────────────────────────
    public List<InventoryDto.Response> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get Low Stock Items ──────────────────────────────────────────────────
    public List<InventoryDto.Response> getLowStockItems() {
        return inventoryRepository.findLowStockItems()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get Inventory for a Product ─────────────────────────────────────────
    public InventoryDto.Response getByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product id: " + productId));
        return toResponse(inventory);
    }

    // ─── Update Inventory ─────────────────────────────────────────────────────
    public InventoryDto.Response updateInventory(Long productId, InventoryDto.UpdateRequest request) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product id: " + productId));

        if (request.getQuantity() != null) {
            inventory.setQuantity(request.getQuantity());
            // Keep product.stockQuantity in sync
            inventory.getProduct().setStockQuantity(request.getQuantity());
        }
        if (request.getThreshold() != null) {
            inventory.setThreshold(request.getThreshold());
        }

        return toResponse(inventoryRepository.save(inventory));
    }

    // ─── Deduct stock (called by OrderService) ────────────────────────────────
    public void deductStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product id: " + productId));

        if (inventory.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock for product id: " + productId
                    + ". Available: " + inventory.getQuantity() + ", Requested: " + quantity);
        }

        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.getProduct().setStockQuantity(inventory.getQuantity());
        inventoryRepository.save(inventory);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private InventoryDto.Response toResponse(Inventory inv) {
        return new InventoryDto.Response(
                inv.getId(),
                inv.getProduct().getId(),
                inv.getProduct().getName(),
                inv.getQuantity(),
                inv.getThreshold(),
                inv.isLowStock()
        );
    }
}
