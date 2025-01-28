package com.localmarket.main.entity.order;

public enum OrderStatus {
    PENDING_PAYMENT,    // Initial state when order is created
    PAYMENT_FAILED,     // Payment attempt failed
    PAYMENT_COMPLETED,  // Payment successful
    PROCESSING,         // Order is being prepared by producer
    SHIPPED,           // Order has been shipped
    DELIVERED,         // Order has been delivered
    CANCELLED,         // Order cancelled (before shipping)
    RETURNED           // Order returned (after delivery)
} 