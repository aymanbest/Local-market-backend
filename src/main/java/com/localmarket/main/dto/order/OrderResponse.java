package com.localmarket.main.dto.order;

import com.localmarket.main.entity.order.Order;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private String token;  // Optional, only present when account is created
    private Order order;
} 