package com.localmarket.main.dto.analytics.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ProducerAnalyticsResponse {

    private int totalOrders;
    private double ordersPercentageChange;
    private double totalRevenue;
    private double revenuePercentageChange;
    private int totalProductsSold;
    private double productsSoldPercentageChange;
    private double growthRate;
    private List<MonthlyData> revenueTrend;
    private List<MonthlyData> monthlyOrders;

}

