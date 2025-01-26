package com.localmarket.main.dto.analytics;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class CategorySalesMetric {
    private String category;
    private long sales;
    private double percentage;
}