package com.localmarket.main.controller.analytics;

import com.localmarket.main.dto.analytics.UserAnalyticsResponse;
import com.localmarket.main.dto.analytics.TransactionAnalyticsResponse;
import com.localmarket.main.dto.analytics.BusinessMetricsResponse;
import com.localmarket.main.service.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.security.AdminOnly;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics APIs (Admin only)")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @Operation(summary = "Get user analytics", description = "Get user-related analytics")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/users")
    @AdminOnly
    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(startDate, endDate));
    }

    @Operation(summary = "Get transaction analytics", description = "Get transaction-related analytics")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/transactions")
    @AdminOnly
    public ResponseEntity<TransactionAnalyticsResponse> getTransactionAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getTransactionAnalytics(startDate, endDate));
    }

    @Operation(summary = "Get business metrics", description = "Get business performance metrics")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/business-metrics")
    @AdminOnly
    public ResponseEntity<BusinessMetricsResponse> getBusinessMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getBusinessMetrics(startDate, endDate));
    }
} 