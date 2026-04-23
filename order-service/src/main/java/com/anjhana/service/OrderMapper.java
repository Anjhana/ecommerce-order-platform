package com.anjhana.service;

import com.anjhana.dto.OrderDtos.*;
import com.anjhana.events.OrderEvents.*;
import com.anjhana.model.Order;
import com.anjhana.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;


@Component
public class OrderMapper {

    public OrderItem toOrderItem(OrderItemRequest req) {
        return OrderItem.builder()
            .skuId(req.skuId())
            .productName(req.productName())
            .quantity(req.quantity())
            .unitPrice(req.unitPrice())
            .build();
    }

    public OrderItemEvent toOrderItemEvent(OrderItem item) {
        return OrderItemEvent.builder()
            .skuId(item.getSkuId())
            .productName(item.getProductName())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .build();
    }

    public OrderResponse toOrderResponse(Order order) {
        var items = order.getItems().stream()
            .map(i -> new OrderItemResponse(
                i.getId(), i.getSkuId(), i.getProductName(),
                i.getQuantity(), i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
            ))
            .collect(Collectors.toList());

        return new OrderResponse(
            order.getId(), order.getCustomerId(),
            order.getStatus().name(), items,
            order.getTotalAmount(), order.getCreatedAt(),
            order.getUpdatedAt(), order.getFailureReason()
        );
    }

    public OrderSummaryResponse toOrderSummaryResponse(Order order) {
        return new OrderSummaryResponse(
            order.getId(), order.getCustomerId(),
            order.getStatus().name(), order.getTotalAmount(), order.getCreatedAt()
        );
    }
}
