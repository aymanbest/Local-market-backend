package com.localmarket.main.dto.order;

import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.entity.payment.PaymentMethod;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String guestEmail;
    private String shippingAddress;
    private String phoneNumber;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private String accessToken;
    private PaymentMethod paymentMethod;
    
    public static OrderResponse fromOrder(Order order) {
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .guestEmail(order.getGuestEmail())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .paymentMethod(order.getPaymentMethod())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .items(order.getItems().stream()
                        .map(OrderItemResponse::fromOrderItem)
                        .collect(Collectors.toList()))
                .accessToken(order.getAccessToken())
                .build();
    }
} 