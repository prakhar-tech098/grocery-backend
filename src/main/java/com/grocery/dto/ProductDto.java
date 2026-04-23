package com.grocery.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class ProductDto {

    // ─── Request (Create / Update) ───────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        @NotBlank(message = "Product name is required")
        private String name;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        private BigDecimal price;

        private String category;
        private String imageUrl;
        private String description;

        @Min(value = 0, message = "Stock quantity cannot be negative")
        private Integer stockQuantity = 0;
    }

    // ─── Response ─────────────────────────────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private BigDecimal price;
        private String category;
        private String imageUrl;
        private String description;
        private Integer stockQuantity;
        private boolean lowStock;
    }
}
