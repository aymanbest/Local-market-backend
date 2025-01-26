package com.localmarket.main.dto.analytics;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthlyRevenue {
    private String month;
    private BigDecimal revenue;
}