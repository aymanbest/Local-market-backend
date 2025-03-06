package com.localmarket.main.dto.analytics.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {
    private int totalOrders;
    private int pendingOrders;
    private int processingOrders;
    private int deliveredOrders;
} 