package com.grocery.service;

import com.grocery.dto.DashboardDto;
import com.grocery.dto.InventoryDto;
import com.grocery.repository.InventoryRepository;
import com.grocery.repository.OrderRepository;
import com.grocery.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private InventoryService inventoryService;

    public DashboardDto getDashboardStats() {
        // Today's date range
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();

        // This month's date range
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        BigDecimal todaySales = orderRepository.sumSalesBetween(todayStart, todayEnd);
        BigDecimal monthSales = orderRepository.sumSalesBetween(monthStart, todayEnd);

        List<InventoryDto.Response> lowStockItems = inventoryService.getLowStockItems();

        return DashboardDto.builder()
                .totalProducts(productRepository.count())
                .totalOrders(orderRepository.count())
                .todaySales(todaySales != null ? todaySales : BigDecimal.ZERO)
                .monthSales(monthSales != null ? monthSales : BigDecimal.ZERO)
                .lowStockCount((long) lowStockItems.size())
                .lowStockItems(lowStockItems)
                .build();
    }
}
