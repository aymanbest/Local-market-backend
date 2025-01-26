package com.localmarket.main.dto.analytics;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserAnalyticsResponse {
    private long totalUsers;
    private long activeProducers;
    private long newUsers;
    private double activeProducersPercentage;
    private double newUsersPercentage;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
} 