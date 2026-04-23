package com.grocery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDto {
    private Long totalProducts;
    private Long totalOrders;
    private BigDecimal todaySales;
    private BigDecimal monthSales;
    private Long lowStockCount;
    private List<InventoryDto.Response> lowStockItems;
}
