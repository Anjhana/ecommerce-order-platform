package com.anjhana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    // ── Request DTOs ──────────────────────────────────────────────────────────

    public record CreateOrderRequest(
        @NotBlank(message = "Customer ID is required")
        String customerId,

        @NotEmpty(message = "Order must have at least one item")
        @Valid
        List<OrderItemRequest> items
    ) {}

    public record OrderItemRequest(
        @NotBlank(message = "SKU ID is required")
        String skuId,

        @NotBlank(message = "Product name is required")
        String productName,

        @Positive(message = "Quantity must be positive")
        Integer quantity,

        @Positive(message = "Unit price must be positive")
        BigDecimal unitPrice
    ) {}

    // ── Response DTOs ─────────────────────────────────────────────────────────

    public record OrderResponse(
        String id,
        String customerId,
        String status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String failureReason
    ) {}

    public record OrderItemResponse(
        String id,
        String skuId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
    ) {}

    public record OrderSummaryResponse(
        String id,
        String customerId,
        String status,
        BigDecimal totalAmount,
        LocalDateTime createdAt
    ) {}

    public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
    ) {}
}
