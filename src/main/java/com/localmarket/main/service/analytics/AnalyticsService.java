package com.localmarket.main.service.analytics;

import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.repository.order.OrderRepository;
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
import com.localmarket.main.dto.analytics.admin.BusinessMetricsResponse;
import com.localmarket.main.dto.analytics.admin.CategorySalesMetric;
import com.localmarket.main.dto.analytics.admin.CombinedAnalyticsResponse;
import com.localmarket.main.dto.analytics.admin.MonthlyRevenue;
import com.localmarket.main.dto.analytics.admin.ProducerPerformance;
import com.localmarket.main.dto.analytics.admin.TransactionAnalyticsResponse;
import com.localmarket.main.dto.analytics.admin.TransactionDetails;
import com.localmarket.main.dto.analytics.admin.UserAnalyticsResponse;
import com.localmarket.main.entity.category.Category;
import org.springframework.beans.factory.annotation.Autowired;
import com.localmarket.main.service.export.CSVExportService;
import com.localmarket.main.service.export.PDFExportService;
import java.util.HashMap;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Autowired
    private CSVExportService csvExportService;

    @Autowired
    private PDFExportService pdfExportService;

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
        List<Order> orders = getOrdersInDateRange(startDate, endDate);
        
        // Handle empty orders list
        if (orders == null || orders.isEmpty()) {
            return TransactionAnalyticsResponse.builder()
                .totalTransactions(0)
                .totalRevenue(BigDecimal.ZERO)
                .averageOrderValue(BigDecimal.ZERO)
                .transactionsByStatus(new HashMap<>())
                .revenueByDay(new HashMap<>())
                .transactions(new ArrayList<>())
                .build();
        }

        Map<OrderStatus, Long> transactionsByStatus = orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        BigDecimal totalRevenue = orders.stream()
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = orders.isEmpty() ? 
            BigDecimal.ZERO : 
            totalRevenue.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);

        Map<LocalDate, BigDecimal> revenueByDay = orders.stream()
            .filter(order -> order.getOrderDate() != null)
            .collect(Collectors.groupingBy(
                (Order order) -> order.getOrderDate().toLocalDate(),
                HashMap::new,
                Collectors.mapping(
                    Order::getTotalPrice,
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                )
            ));

        List<TransactionDetails> transactions = orders.stream()
            .filter(order -> !order.getItems().isEmpty())
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
            .totalTransactions(orders.size())
            .totalRevenue(totalRevenue)
            .averageOrderValue(averageOrderValue)
            .transactionsByStatus(transactionsByStatus)
            .transactions(transactions)
            .revenueByDay(revenueByDay)
            .build();
    }

    private List<Order> getOrdersInDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        return orderRepository.findByOrderDateBetween(
            startDate.atStartOfDay(),
            endDate.atTime(LocalTime.MAX)
        );
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

    public byte[] exportAnalytics(LocalDate startDate, LocalDate endDate, String format) {
        CombinedAnalyticsResponse analytics = getCombinedAnalytics(startDate, endDate);
        
        if (format.equalsIgnoreCase("pdf")) {
            return pdfExportService.generatePDF(analytics);
        }
        return csvExportService.generateCSV(analytics);
    }

    private CombinedAnalyticsResponse getCombinedAnalytics(LocalDate startDate, LocalDate endDate) {
        UserAnalyticsResponse userAnalytics = getUserAnalytics(startDate, endDate);
        TransactionAnalyticsResponse transactionAnalytics = getTransactionAnalytics(startDate, endDate);
        BusinessMetricsResponse businessMetrics = getBusinessMetrics(startDate, endDate);

        return CombinedAnalyticsResponse.builder()
            // User Analytics
            .totalUsers(userAnalytics.getTotalUsers())
            .activeProducers(userAnalytics.getActiveProducers())
            .newUsers(userAnalytics.getNewUsers())
            .activeProducersPercentage(userAnalytics.getActiveProducersPercentage())
            .newUsersPercentage(userAnalytics.getNewUsersPercentage())
            .periodStart(userAnalytics.getPeriodStart())
            .periodEnd(userAnalytics.getPeriodEnd())
            // Transaction Analytics
            .totalTransactions(transactionAnalytics.getTotalTransactions())
            .totalRevenue(transactionAnalytics.getTotalRevenue())
            .averageOrderValue(transactionAnalytics.getAverageOrderValue())
            .transactionsByStatus(transactionAnalytics.getTransactionsByStatus())
            .revenueByDay(transactionAnalytics.getRevenueByDay())
            // Business Metrics
            .totalRevenue(businessMetrics.getTotalRevenue())
            .revenueGrowthRate(businessMetrics.getRevenueGrowthRate())
            .activeUsers(businessMetrics.getActiveUsers())
            .activeUsersGrowthRate(businessMetrics.getActiveUsersGrowthRate())
            .totalSales(businessMetrics.getTotalSales())
            .salesGrowthRate(businessMetrics.getSalesGrowthRate())
            .overallGrowthRate(businessMetrics.getOverallGrowthRate())
            .salesByCategory(businessMetrics.getSalesByCategory())
            .revenueByMonth(businessMetrics.getRevenueByMonth())
            .topProducers(businessMetrics.getTopProducers())
            .build();
    }
} 