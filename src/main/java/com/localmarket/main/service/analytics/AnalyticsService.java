package com.localmarket.main.service.analytics;

import com.localmarket.main.dto.analytics.UserAnalyticsResponse;
import com.localmarket.main.dto.analytics.TransactionAnalyticsResponse;
import com.localmarket.main.dto.analytics.BusinessMetricsResponse;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.dto.analytics.CategorySalesMetric;
import com.localmarket.main.dto.analytics.MonthlyRevenue;
import com.localmarket.main.dto.analytics.ProducerPerformance;
import com.localmarket.main.dto.analytics.TransactionDetails;
import com.localmarket.main.entity.category.Category;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UserAnalyticsResponse getUserAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() 
            : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) 
            : LocalDateTime.now();

        long totalUsers = userRepository.countByCreatedAtBefore(end);
        long activeProducers = userRepository.countByRoleAndCreatedAtBetween(Role.PRODUCER, start, end);
        long newUsers = userRepository.countByCreatedAtBetween(start, end);
        
        double activeProducersPercentage = (double) activeProducers / totalUsers * 100;
        double newUsersPercentage = (double) newUsers / totalUsers * 100;

        return UserAnalyticsResponse.builder()
            .totalUsers(totalUsers)
            .activeProducers(activeProducers)
            .newUsers(newUsers)
            .activeProducersPercentage(activeProducersPercentage)
            .newUsersPercentage(newUsersPercentage)
            .periodStart(start)
            .periodEnd(end)
            .build();
    }

    public TransactionAnalyticsResponse getTransactionAnalytics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() 
            : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) 
            : LocalDateTime.now();

        List<Order> orders = orderRepository.findByOrderDateBetween(start, end);
        
        BigDecimal totalVolume = orders.stream()
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        long completedCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
            .count();
            
        long pendingCount = orders.stream()
            .filter(o -> o.getStatus() == OrderStatus.PENDING)
            .count();

        List<TransactionDetails> transactions = orders.stream()
            .map(order -> TransactionDetails.builder()
                .orderId(order.getOrderId())
                .transactionId(order.getPayment() != null ? order.getPayment().getTransactionId() : null)
                .customerName(order.getCustomer() != null ? 
                    order.getCustomer().getFirstname() + " " + order.getCustomer().getLastname() : 
                    order.getGuestEmail())
                .producerName(order.getItems().get(0).getProduct().getProducer().getUsername())
                .amount(order.getTotalPrice())
                .status(order.getStatus().toString())
                .date(order.getOrderDate())
                .build())
            .collect(Collectors.toList());

        return TransactionAnalyticsResponse.builder()
            .totalTransactionVolume(totalVolume)
            .completedTransactions(completedCount)
            .pendingTransactions(pendingCount)
            .transactions(transactions)
            .build();
    }

    public BusinessMetricsResponse getBusinessMetrics(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() 
            : LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) 
            : LocalDateTime.now();

        // Calculate current period metrics
        List<Order> currentPeriodOrders = orderRepository.findByOrderDateBetween(start, end);
        BigDecimal currentRevenue = currentPeriodOrders.stream()
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate previous period metrics for growth rate
        long periodDays = ChronoUnit.DAYS.between(start, end);
        LocalDateTime previousStart = start.minusDays(periodDays);
        List<Order> previousPeriodOrders = orderRepository.findByOrderDateBetween(previousStart, start);
        BigDecimal previousRevenue = previousPeriodOrders.stream()
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate growth rates
        double revenueGrowthRate = calculateGrowthRate(previousRevenue, currentRevenue);
        
        long activeUsers = userRepository.countByLastLoginBetween(start, end);
        long previousActiveUsers = userRepository.countByLastLoginBetween(previousStart, start);
        double activeUsersGrowthRate = calculateGrowthRate(previousActiveUsers, activeUsers);

        long totalSales = currentPeriodOrders.size();
        long previousSales = previousPeriodOrders.size();
        double salesGrowthRate = calculateGrowthRate(previousSales, totalSales);

        // Calculate sales by category
        List<CategorySalesMetric> salesByCategory = calculateSalesByCategory(currentPeriodOrders);

        // Calculate monthly revenue trend
        List<MonthlyRevenue> revenueByMonth = calculateMonthlyRevenue(start, end);

        // Calculate top producers performance
        List<ProducerPerformance> topProducers = calculateTopProducersPerformance(start, end);

        return BusinessMetricsResponse.builder()
            .totalRevenue(currentRevenue)
            .revenueGrowthRate(revenueGrowthRate)
            .activeUsers(activeUsers)
            .activeUsersGrowthRate(activeUsersGrowthRate)
            .totalSales(totalSales)
            .salesGrowthRate(salesGrowthRate)
            .overallGrowthRate((revenueGrowthRate + activeUsersGrowthRate + salesGrowthRate) / 3)
            .salesByCategory(salesByCategory)
            .revenueByMonth(revenueByMonth)
            .topProducers(topProducers)
            .build();
    }

    private double calculateGrowthRate(Number previous, Number current) {
        if (previous.doubleValue() == 0) return 100.0;
        return ((current.doubleValue() - previous.doubleValue()) / previous.doubleValue()) * 100;
    }

    private List<CategorySalesMetric> calculateSalesByCategory(List<Order> orders) {
        Map<Category, Long> categorySales = orders.stream()
            .flatMap(order -> order.getItems().stream())
            .collect(Collectors.groupingBy(
                item -> item.getProduct().getCategories().iterator().next(),
                Collectors.counting()
            ));

        long totalSales = categorySales.values().stream().mapToLong(Long::valueOf).sum();

        return categorySales.entrySet().stream()
            .map(entry -> CategorySalesMetric.builder()
                .category(entry.getKey().getName())
                .sales(entry.getValue())
                .percentage((double) entry.getValue() / totalSales * 100)
                .build())
            .collect(Collectors.toList());
    }

    private List<MonthlyRevenue> calculateMonthlyRevenue(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findMonthlyRevenue(start, end).stream()
            .map(tuple -> MonthlyRevenue.builder()
                .month(tuple.get("month", String.class))
                .revenue(tuple.get("revenue", BigDecimal.class))
                .build())
            .collect(Collectors.toList());
    }

    private List<ProducerPerformance> calculateTopProducersPerformance(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findProducerPerformance(start, end).stream()
            .map(tuple -> ProducerPerformance.builder()
                .producerName(tuple.get("producerName", String.class))
                .totalSales(tuple.get("totalSales", Long.class))
                .totalRevenue(tuple.get("totalRevenue", BigDecimal.class))
                .growthRate(tuple.get("growthRate", BigDecimal.class).doubleValue())
                .build())
            .collect(Collectors.toList());
    }

} 