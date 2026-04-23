package com.anjhana.service;

import com.anjhana.dto.OrderDtos.*;
import com.anjhana.events.OrderEvents.*;
import com.anjhana.exception.OrderNotFoundException;
import com.anjhana.model.Order;
import com.anjhana.model.OrderItem;
import com.anjhana.model.OrderStatus;
import com.anjhana.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderMapper orderMapper;

    private static final String TOPIC_ORDER_CREATED   = "order.created";
    private static final String TOPIC_ORDER_CONFIRMED = "order.confirmed";
    private static final String TOPIC_ORDER_CANCELLED = "order.cancelled";

    // ── CREATE ──────────────────────────────────────────────────────────────

    /**
     * Create a new order and start the SAGA by publishing OrderCreatedEvent.
     * InventoryService will pick this up next.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.customerId());

        // Map request → entity
        List<OrderItem> items = request.items().stream()
            .map(orderMapper::toOrderItem)
            .collect(Collectors.toList());

        Order order = Order.builder()
            .customerId(request.customerId())
            .items(items)
            .status(OrderStatus.PENDING)
            .build();

        order.recalculateTotal();
        Order saved = orderRepository.save(order);

        log.info("Order saved: {} for customer: {} total: {}",
                 saved.getId(), saved.getCustomerId(), saved.getTotalAmount());

        // Publish SAGA start event — InventoryService will react
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(saved.getId())
            .customerId(saved.getCustomerId())
            .totalAmount(saved.getTotalAmount())
            .items(items.stream().map(orderMapper::toOrderItemEvent).collect(Collectors.toList()))
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send(TOPIC_ORDER_CREATED, saved.getId(), event);
        log.info("Published OrderCreatedEvent for orderId: {}", saved.getId());

        return orderMapper.toOrderResponse(saved);
    }


    /**
     * Called when InventoryService successfully reserved stock.
     * Update status → payment will be triggered by PaymentService listening to order.confirmed
     */
    @Transactional
    public void handleInventoryReserved(Map<String,Object> event) {
        log.info("Inventory reserved for orderId: {}", event.get("orderId"));
        updateOrderStatus(event.get("orderId").toString(), OrderStatus.INVENTORY_RESERVED, null);
    }

    /**
     * Called when InventoryService failed to reserve stock.
     * SAGA compensation: cancel the order.
     */
    @Transactional
    public void handleInventoryFailed(Map<String,Object> event) {
        log.warn("Inventory failed for orderId: {} reason: {}", event.get("orderId").toString(), event.get("reason").toString());
        cancelOrder(event.get("orderId").toString(), "Inventory reservation failed: " + event.get("reason").toString());
    }

    /**
     * Called when PaymentService successfully charged the customer.
     * Order is now confirmed — publish OrderConfirmedEvent for NotificationService.
     */
    @Transactional
    public void handlePaymentProcessed(Map<String,Object> event) {
        log.info("Payment processed for orderId: {} txId: {}", event.get("orderId").toString(), event.get("transactionId").toString());
        Order order = updateOrderStatus(event.get("orderId").toString(), OrderStatus.CONFIRMED, null);

        OrderConfirmedEvent confirmedEvent = OrderConfirmedEvent.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .totalAmount(order.getTotalAmount())
            .timestamp(LocalDateTime.now())
            .build();
        kafkaTemplate.send(TOPIC_ORDER_CONFIRMED, order.getId(), confirmedEvent);
    }

    /**
     * Called when PaymentService failed.
     * SAGA compensation: inventory was already reserved — publish cancel so it releases stock.
     */
    @Transactional
    public void handlePaymentFailed(Map<String,Object> event) {
        log.warn("Payment failed for orderId: {} reason: {}", event.get("orderId").toString(), event.get("reason").toString());
        cancelOrder(event.get("orderId").toString(), "Payment failed: " + event.get("reason").toString());
    }

    // ── QUERIES ──────────────────────────────────────────────────────────────

    public OrderResponse getOrder(String orderId) {
        Order order = findOrThrow(orderId);
        return orderMapper.toOrderResponse(order);
    }

    public List<OrderSummaryResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
            .stream()
            .map(orderMapper::toOrderSummaryResponse)
            .collect(Collectors.toList());
    }

    public List<OrderSummaryResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status)
            .stream()
            .map(orderMapper::toOrderSummaryResponse)
            .collect(Collectors.toList());
    }

  

    private Order updateOrderStatus(String orderId, OrderStatus newStatus, String reason) {
        Order order = findOrThrow(orderId);
        order.setStatus(newStatus);
        order.setFailureReason(reason);
        return orderRepository.save(order);
    }

    private void cancelOrder(String orderId, String reason) {
        Order order = updateOrderStatus(orderId, OrderStatus.CANCELLED, reason);

        OrderCancelledEvent event = OrderCancelledEvent.builder()
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .reason(reason)
            .timestamp(LocalDateTime.now())
            .build();
        kafkaTemplate.send(TOPIC_ORDER_CANCELLED, order.getId(), event);
        log.info("Order {} cancelled. Published OrderCancelledEvent.", orderId);
    }

    private Order findOrThrow(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }
}
