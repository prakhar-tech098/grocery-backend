package com.grocery.controller;

import com.grocery.dto.ApiResponse;
import com.grocery.dto.OrderDto;
import com.grocery.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * GET /api/orders
     * Returns all orders, newest first
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDto.Response>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders()));
    }

    /**
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto.Response>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id)));
    }

    /**
     * POST /api/orders
     * Creates a new order / bill — deducts inventory automatically
     *
     * Request body:
     * {
     *   "customerName": "John Doe",
     *   "items": [
     *     { "productId": 1, "quantity": 3 },
     *     { "productId": 4, "quantity": 1 }
     *   ]
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderDto.Response>> createOrder(
            @Valid @RequestBody OrderDto.CreateRequest request) {
        OrderDto.Response order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", order));
    }

    /**
     * PUT /api/orders/{id}/cancel
     * Cancels an order and restores inventory
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDto.Response>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Order cancelled", orderService.cancelOrder(id)));
    }
}
