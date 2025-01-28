package com.localmarket.main.service.analytics.producer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.localmarket.main.dto.analytics.producer.MonthlyData;
import com.localmarket.main.dto.analytics.producer.ProducerAnalyticsResponse;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.repository.order.OrderItemRepository;
import com.localmarket.main.repository.order.OrderRepository;
    

@Service
public class ProducerAnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public ProducerAnalyticsService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public ProducerAnalyticsResponse getAnalyticsOverview(LocalDateTime startOfCurrentPeriod, LocalDateTime endOfCurrentPeriod, LocalDateTime startOfPreviousPeriod, LocalDateTime endOfPreviousPeriod) {
        // Fetch current period data
        int totalOrders = orderRepository.countAllOrders();
        double totalRevenue = orderRepository.calculateTotalRevenue() != null ? orderRepository.calculateTotalRevenue() : 0.0;
        int totalProductsSold = orderItemRepository.countTotalProductsSoldInDateRange(startOfCurrentPeriod, endOfCurrentPeriod) != null
                ? orderItemRepository.countTotalProductsSoldInDateRange(startOfCurrentPeriod, endOfCurrentPeriod)
                : 0;

        // Fetch previous period data
        int prevTotalOrders = orderRepository.countAllOrdersPreviousPeriod(startOfPreviousPeriod, endOfPreviousPeriod);
        Double prevTotalRevenue = orderRepository.calculateTotalRevenuePreviousPeriod(startOfPreviousPeriod, endOfPreviousPeriod);
        double prevTotalRevenueValue = (prevTotalRevenue != null) ? prevTotalRevenue : 0.0;
        int prevTotalProductsSold = orderItemRepository.countTotalProductsSoldInDateRange(startOfPreviousPeriod, endOfPreviousPeriod) != null
                ? orderItemRepository.countTotalProductsSoldInDateRange(startOfPreviousPeriod, endOfPreviousPeriod)
                : 0;

        // Fetch Revenue Trend (Monthly Revenue for current year)
        List<Object[]> revenueTrendData = orderRepository.calculateRevenueTrend(startOfCurrentPeriod.withDayOfYear(1), endOfCurrentPeriod);
        List<MonthlyData> revenueTrend = mapToMonthlyData(revenueTrendData);

        // Fetch Monthly Orders (Orders count for each month in the current year)
        List<Object[]> monthlyOrdersData = orderRepository.calculateMonthlyOrders(startOfCurrentPeriod.withDayOfYear(1), endOfCurrentPeriod);
        List<MonthlyData> monthlyOrders = mapToMonthlyData(monthlyOrdersData);

        // Calculate percentage changes
        double ordersPercentageChange = calculatePercentageChange(totalOrders, prevTotalOrders);
        double revenuePercentageChange = calculatePercentageChange(totalRevenue, prevTotalRevenueValue);
        double productsSoldPercentageChange = calculatePercentageChange(totalProductsSold, prevTotalProductsSold);

        // Calculate growth rate (example: revenue-based growth)
        double growthRate = revenuePercentageChange;

        return new ProducerAnalyticsResponse(
            totalOrders, ordersPercentageChange,
            totalRevenue, revenuePercentageChange,
            totalProductsSold, productsSoldPercentageChange,
            growthRate,
            revenueTrend,
            monthlyOrders
        );
    }

    // New methods for total orders by status
    public int getTotalOrders() {
        return orderRepository.countAllOrders();
    }

    public int getTotalOrdersByStatus(OrderStatus status) {
        return orderRepository.countOrdersByStatus(status);
    }

    // Mapping method for monthly data
    private List<MonthlyData> mapToMonthlyData(List<Object[]> data) {
        return data.stream()
                .map(row -> new MonthlyData((Integer) row[0], ((Number) row[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    private double calculatePercentageChange(double current, double previous) {
        if (previous == 0) return current > 0 ? 100 : 0;
        return ((current - previous) / previous) * 100;
    }
}
