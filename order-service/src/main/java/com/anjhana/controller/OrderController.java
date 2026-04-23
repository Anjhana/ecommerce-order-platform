package com.anjhana.controller;

import com.anjhana.dto.OrderDtos.*;
import com.anjhana.model.OrderStatus;
import com.anjhana.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Order Service.
 *
 * Endpoints:
 *   POST   /api/orders                          - Create order (starts SAGA)
 *   GET    /api/orders/{orderId}                - Get order by ID
 *   GET    /api/orders/customer/{customerId}    - All orders for a customer
 *   GET    /api/orders/status/{status}          - Orders by status
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("POST /api/orders - customer: {}", request.customerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable(name="orderId") String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersByCustomer(
            @PathVariable(name="customerId") String customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersByStatus(
            @PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }
}
