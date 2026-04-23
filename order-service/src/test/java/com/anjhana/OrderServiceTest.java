package com.anjhana;

import com.anjhana.dto.OrderDtos.*;
import com.anjhana.model.Order;
import com.anjhana.model.OrderStatus;
import com.anjhana.repository.OrderRepository;
import com.anjhana.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private com.anjhana.service.OrderMapper orderMapper;
    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_shouldSaveAndPublishEvent() {
        // Arrange
        var itemReq = new OrderDtos.OrderItemRequest("SKU-001", "Widget", 2, BigDecimal.valueOf(50));
        var request = new CreateOrderRequest("CUST-001", List.of(itemReq));

        var savedOrder = Order.builder()
            .id("ORD-001").customerId("CUST-001")
            .totalAmount(BigDecimal.valueOf(100)).build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toOrderItem(any())).thenReturn(new com.anjhana.model.OrderItem());
        when(orderMapper.toOrderItemEvent(any())).thenReturn(mock(com.anjhana.events.OrderEvents.OrderItemEvent.class));
        when(orderMapper.toOrderResponse(any())).thenReturn(
            new OrderResponse("ORD-001","CUST-001","PENDING",List.of(),BigDecimal.valueOf(100),null,null,null));

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertThat(result.id()).isEqualTo("ORD-001");
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order.created"), eq("ORD-001"), any());
    }

    @Test
    void getOrder_whenNotFound_shouldThrow() {
        when(orderRepository.findById("MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrder("MISSING"))
            .isInstanceOf(com.anjhana.exception.OrderNotFoundException.class);
    }

    @Test
    void handleInventoryFailed_shouldCancelOrder() {
        var order = Order.builder().id("ORD-001").customerId("CUST-001")
            .status(OrderStatus.PENDING).totalAmount(BigDecimal.valueOf(100)).build();

        when(orderRepository.findById("ORD-001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(order);

        var failEvent = com.anjhana.events.OrderEvents.InventoryFailedEvent.builder()
            .orderId("ORD-001").reason("Out of stock").build();

        orderService.handleInventoryFailed(failEvent);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(kafkaTemplate).send(eq("order.cancelled"), eq("ORD-001"), any());
    }
}
