package com.localmarket.main.dto.analytics.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.localmarket.main.entity.order.OrderStatus;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAnalyticsResponse {
    private int totalTransactions;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Map<OrderStatus, Long> transactionsByStatus;
    private Map<LocalDate, BigDecimal> revenueByDay;
    private List<TransactionDetails> transactions;
}

