package com.anjhana.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Kafka event payloads for SAGA choreography.
 *
 * SAGA Flow (Choreography — event-driven, no central orchestrator):
 *
 *   OrderService    → publishes  OrderCreatedEvent        → topic: order.created
 *   InventoryService listens   → reserves stock
 *                  → publishes  InventoryReservedEvent    → topic: inventory.reserved
 *                  OR publishes InventoryFailedEvent      → topic: inventory.failed
 *   PaymentService  listens    → charges payment
 *                  → publishes  PaymentProcessedEvent     → topic: payment.processed
 *                  OR publishes PaymentFailedEvent        → topic: payment.failed
 *   OrderService    listens    → confirms or cancels
 *   NotificationService listens → sends email at each step
 */
public class OrderEvents {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderCreatedEvent {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private List<OrderItemEvent> items;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemEvent {
        private String skuId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InventoryReservedEvent {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InventoryFailedEvent {
        private String orderId;
        private String reason;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentProcessedEvent {
        private String orderId;
        private String transactionId;
        private BigDecimal amount;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentFailedEvent {
        private String orderId;
        private String reason;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderConfirmedEvent {
        private String orderId;
        private String customerId;
        private BigDecimal totalAmount;
        private LocalDateTime timestamp;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderCancelledEvent {
        private String orderId;
        private String customerId;
        private String reason;
        private LocalDateTime timestamp;
    }
}
