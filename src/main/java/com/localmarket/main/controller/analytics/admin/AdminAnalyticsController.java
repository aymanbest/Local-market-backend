package com.localmarket.main.controller.analytics.admin;

import com.localmarket.main.service.analytics.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.localmarket.main.dto.analytics.admin.BusinessMetricsResponse;
import com.localmarket.main.dto.analytics.admin.TransactionAnalyticsResponse;
import com.localmarket.main.dto.analytics.admin.UserAnalyticsResponse;
import com.localmarket.main.security.AdminOnly;
import java.time.LocalDate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics Admin", description = "Analytics APIs (Admin only)")
public class AdminAnalyticsController {
    private final AnalyticsService analyticsService;

    @Operation(summary = "Get user analytics", description = "Get user-related analytics")
    @SecurityRequirement(name = "cookie")
    @GetMapping("/users")
    @AdminOnly
    public ResponseEntity<UserAnalyticsResponse> getUserAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getUserAnalytics(startDate, endDate));
    }

    @Operation(summary = "Get transaction analytics", description = "Get transaction-related analytics")
    @SecurityRequirement(name = "cookie")
    @GetMapping("/transactions")
    @AdminOnly
    public ResponseEntity<TransactionAnalyticsResponse> getTransactionAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            return ResponseEntity.ok(analyticsService.getTransactionAnalytics(startDate, endDate));
        } catch (Exception e) {
            throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, 
                "Error retrieving transaction analytics: " + e.getMessage());
        }
    }

    @Operation(summary = "Get business metrics", description = "Get business performance metrics")
    @SecurityRequirement(name = "cookie")
    @GetMapping("/business-metrics")
    @AdminOnly
    public ResponseEntity<BusinessMetricsResponse> getBusinessMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getBusinessMetrics(startDate, endDate));
    }

    @Operation(summary = "Export analytics", description = "Export combined analytics data as CSV or PDF")
    @SecurityRequirement(name = "cookie")
    @GetMapping("/export")
    @AdminOnly
    public ResponseEntity<Resource> exportAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format) {
        
        byte[] data = analyticsService.exportAnalytics(startDate, endDate, format);
        String filename = "analytics_report_" + LocalDate.now();
        String contentType;
        
        if (format.equalsIgnoreCase("pdf")) {
            filename += ".pdf";
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else {
            filename += ".csv";
            contentType = "text/csv";
        }

        ByteArrayResource resource = new ByteArrayResource(data);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
} 