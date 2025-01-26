package com.localmarket.main.dto.analytics;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProducerPerformance {
    private String producerName;
    private long totalSales;
    private BigDecimal totalRevenue;
    private double growthRate;
} 
