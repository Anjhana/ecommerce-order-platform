package com.anjhana.config;

import com.anjhana.events.OrderEvents.*;
import com.anjhana.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to SAGA outcome events from Inventory and Payment services.
 * Routes to OrderService for state machine transitions.
 *
 * Topics consumed:
 *   inventory.reserved  - stock reserved OK  → advance SAGA
 *   inventory.failed    - no stock           → compensate (cancel)
 *   payment.processed   - charged OK         → confirm order
 *   payment.failed      - charge failed      → compensate (cancel + release inventory)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service")
    public void onInventoryReserved(Map<String,Object> event) {
        log.info("Received InventoryReservedEvent for orderId: {}", event.get("orderId").toString());
        orderService.handleInventoryReserved(event);
    }

    @KafkaListener(topics = "inventory.failed", groupId = "order-service")
    public void onInventoryFailed(Map<String,Object> event) {
        log.warn("Received InventoryFailedEvent for orderId: {}", event.get("orderId").toString());
        orderService.handleInventoryFailed(event);
    }

    @KafkaListener(topics = "payment.processed", groupId = "order-service")
    public void onPaymentProcessed(Map<String,Object> event) {
        log.info("Received PaymentProcessedEvent for orderId: {}", event.get("orderId").toString());
        orderService.handlePaymentProcessed(event);
    }

    @KafkaListener(topics = "payment.failed", groupId = "order-service")
    public void onPaymentFailed(Map<String,Object> event) {
        log.warn("Received PaymentFailedEvent for orderId: {}", event.get("orderId").toString());
        orderService.handlePaymentFailed(event);
    }
}
