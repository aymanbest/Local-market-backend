package com.localmarket.main.dto.analytics;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CombinedAnalyticsResponse {
    // User Analytics
    private long totalUsers;
    private long activeProducers;
    private long newUsers;
    private double activeProducersPercentage;
    private double newUsersPercentage;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // Transaction Analytics
    private BigDecimal totalTransactionVolume;
    private long completedTransactions;
    private long pendingTransactions;
    private long totalTransactions;

    // Business Metrics
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