package com.anjhana.model;


public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    INVENTORY_FAILED,
    PAYMENT_PROCESSING,
    PAYMENT_FAILED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
