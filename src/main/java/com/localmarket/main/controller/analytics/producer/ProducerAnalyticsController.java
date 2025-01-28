package com.localmarket.main.controller.analytics.producer;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.localmarket.main.dto.analytics.producer.ProducerAnalyticsResponse;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.security.ProducerOnly;
import com.localmarket.main.service.analytics.producer.ProducerAnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/analytics")
public class ProducerAnalyticsController {

    private final ProducerAnalyticsService producerAnalyticsService;

    public ProducerAnalyticsController(ProducerAnalyticsService producerAnalyticsService) {
        this.producerAnalyticsService = producerAnalyticsService;
    }

    @ProducerOnly
    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview", description = "Provides a summary of total orders, revenue, products sold, and growth rate for the current and previous months.")
    public ProducerAnalyticsResponse getAnalyticsOverview() {
        // Define time periods
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfCurrentMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay();
        LocalDateTime endOfCurrentMonth = now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59, 59);
        LocalDateTime startOfPreviousMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPreviousMonth = startOfPreviousMonth.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59, 59);

        return producerAnalyticsService.getAnalyticsOverview(
            startOfCurrentMonth,
            endOfCurrentMonth,
            startOfPreviousMonth,
            endOfPreviousMonth
        );
    }

    @ProducerOnly
    @GetMapping("/total-orders")
    @Operation(summary = "Get total orders", description = "Returns the total number of orders placed.")
    public int getTotalOrders() {
        return producerAnalyticsService.getTotalOrders();
    }

    @ProducerOnly
    @GetMapping("/total-pending-orders")
    @Operation(summary = "Get total pending orders", description = "Returns the total number of pending orders.")
    public int getTotalPendingOrders() {
        return producerAnalyticsService.getTotalOrdersByStatus(OrderStatus.PENDING_PAYMENT);
    }

    @ProducerOnly
    @GetMapping("/total-delivered-orders")
    @Operation(summary = "Get total delivered orders", description = "Returns the total number of delivered orders.")
    public int getTotalDeliveredOrders() {
        return producerAnalyticsService.getTotalOrdersByStatus(OrderStatus.DELIVERED);
    }

    @ProducerOnly
    @GetMapping("/total-processing-orders")
    @Operation(summary = "Get total processing orders", description = "Returns the total number of processing orders.")
    public int getTotalProcessingOrders() {
        return producerAnalyticsService.getTotalOrdersByStatus(OrderStatus.PROCESSING);
    }
}
