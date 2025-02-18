package com.localmarket.main.dto.analytics.admin;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import com.localmarket.main.entity.order.OrderStatus;
import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private BigDecimal averageOrderValue;
    private Map<OrderStatus, Long> transactionsByStatus;
    private Map<LocalDate, BigDecimal> revenueByDay;
} 