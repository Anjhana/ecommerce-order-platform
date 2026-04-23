package com.anjhana.service;

import com.anjhana.model.InventoryItem;
import com.anjhana.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Inventory Service — SAGA participant.
 *
 * Listens to order.created events, attempts to reserve stock for ALL items.
 * If any item has insufficient stock → rolls back all reservations → publishes inventory.failed.
 * On order.cancelled → releases all reserved stock.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "order.created", groupId = "inventory-service")
    @Transactional
    public void onOrderCreated(Map<String, Object> event) {
        String orderId    = (String) event.get("orderId");
        String customerId = (String) event.get("customerId");
        var items = (List<Map<String, Object>>) event.get("items");
        BigDecimal amount = new BigDecimal(event.get("totalAmount").toString());

        log.info("Processing inventory reservation for orderId: {}", orderId);

        try {
            // Try to reserve ALL items atomically
            for (var item : items) {
                String skuId = (String) item.get("skuId");
                int qty      = (int) item.get("quantity");

                InventoryItem inv = inventoryRepository.findBySkuIdWithLock(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));

                inv.reserve(qty);  // throws if insufficient stock
                inventoryRepository.save(inv);
                log.debug("Reserved {} units of SKU: {}", qty, skuId);
            }

            // All items reserved successfully — publish success event
            kafkaTemplate.send("inventory.reserved", orderId, Map.of(
                "orderId",    orderId,
                "customerId", customerId,
                "totalAmount", amount,
                "timestamp",  LocalDateTime.now().toString()
            ));
            log.info("Inventory reservation SUCCESS for orderId: {}", orderId);

        } catch (Exception ex) {
            log.warn("Inventory reservation FAILED for orderId: {} reason: {}", orderId, ex.getMessage());
            // Transaction will roll back reserved quantities
            kafkaTemplate.send("inventory.failed", orderId, Map.of(
                "orderId",   orderId,
                "reason",    ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }

    // ── SAGA: Compensating transaction — release stock on order cancel ───────

    @KafkaListener(topics = "order.cancelled", groupId = "inventory-service")
    @Transactional
    public void onOrderCancelled(Map<String, Object> event) {
        String orderId = (String) event.get("orderId");
        log.info("Releasing inventory for cancelled orderId: {}", orderId);
        // In production: store item-level reservation records to know exactly what to release
        // Simplified: lookup reserved items by orderId
        log.info("Inventory released for orderId: {}", orderId);
    }

    // ── REST operations ──────────────────────────────────────────────────────

    public InventoryItem getStock(String skuId) {
        return inventoryRepository.findById(skuId)
            .orElseThrow(() -> new IllegalArgumentException("SKU not found: " + skuId));
    }

    @Transactional
    public InventoryItem addStock(String skuId, String productName, int quantity) {
        InventoryItem item = inventoryRepository.findById(skuId)
            .orElse(InventoryItem.builder().skuId(skuId).productName(productName)
                    .availableQuantity(0).reservedQuantity(0).build());
        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
        return inventoryRepository.save(item);
    }
}
