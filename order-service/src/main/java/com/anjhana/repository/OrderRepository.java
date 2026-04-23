package com.anjhana.repository;

import com.anjhana.model.Order;
import com.anjhana.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'CONFIRMED' AND o.createdAt >= :from")
    BigDecimal sumRevenueFrom(LocalDateTime from);

    long countByStatus(OrderStatus status);
}
