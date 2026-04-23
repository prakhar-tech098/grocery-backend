package com.grocery.service;

import com.grocery.dto.OrderDto;
import com.grocery.entity.*;
import com.grocery.exception.ResourceNotFoundException;
import com.grocery.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private ProductService productService;
    @Autowired private InventoryService inventoryService;

    // ─── Get All Orders ───────────────────────────────────────────────────────
    public List<OrderDto.Response> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Get Order By ID ──────────────────────────────────────────────────────
    public OrderDto.Response getOrderById(Long id) {
        Order order = findOrderById(id);
        return toResponse(order);
    }

    // ─── Create Order (Billing) ───────────────────────────────────────────────
    public OrderDto.Response createOrder(OrderDto.CreateRequest request) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Build order items and calculate total
        for (OrderDto.ItemRequest itemRequest : request.getItems()) {
            Product product = productService.findProductById(itemRequest.getProductId());

            // Deduct from inventory (throws if insufficient stock)
            inventoryService.deductStock(product.getId(), itemRequest.getQuantity());

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .build();

            totalAmount = totalAmount.add(item.getSubtotal());
            orderItems.add(item);
        }

        // Save the order
        Order order = Order.builder()
                .customerName(request.getCustomerName())
                .totalAmount(totalAmount)
                .status(Order.OrderStatus.COMPLETED)
                .build();

        order = orderRepository.save(order);

        // Link items to order
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        order.getItems().addAll(orderItems);
        order = orderRepository.save(order);

        return toResponse(order);
    }

    // ─── Cancel Order ─────────────────────────────────────────────────────────
    public OrderDto.Response cancelOrder(Long id) {
        Order order = findOrderById(id);
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        // Restore stock on cancellation
        for (OrderItem item : order.getItems()) {
            inventoryService.deductStock(item.getProduct().getId(), -item.getQuantity());
        }
        return toResponse(orderRepository.save(order));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private Order findOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
    }

    private OrderDto.Response toResponse(Order order) {
        List<OrderDto.ItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new OrderDto.ItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderDto.Response(
                order.getId(),
                order.getCustomerName(),
                order.getTotalAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}
