package com.localmarket.main.dto.analytics.producer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyData {
    private int month; // e.g., 1 for January, 2 for February
    private double value; // e.g., total revenue or total orders
}

