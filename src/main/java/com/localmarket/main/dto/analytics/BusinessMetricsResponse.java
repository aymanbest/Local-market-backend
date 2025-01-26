package com.localmarket.main.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class BusinessMetricsResponse {
    private BigDecimal totalRevenue;
    private double revenueGrowthRate;
    private long activeUsers;
    private double activeUsersGrowthRate;
    private long totalSales;
    private double salesGrowthRate;
    private double overallGrowthRate;
    private List<CategorySalesMetric> salesByCategory;
    private List<MonthlyRevenue> revenueByMonth;
    private List<ProducerPerformance> topProducers;
}



