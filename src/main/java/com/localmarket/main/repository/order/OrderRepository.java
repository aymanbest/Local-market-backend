package com.localmarket.main.repository.order;

import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.order.OrderItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.localmarket.main.entity.order.OrderStatus;
import jakarta.persistence.Tuple;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerUserId(Long userId);
    Page<Order> findByCustomerUserId(Long userId, Pageable pageable);

    List<Order> findByCustomerUserIdAndStatus(Long userId, OrderStatus status);

    Optional<Order> findByOrderIdAndCustomerUserId(Long orderId, Long userId);

    @Query(value = """
            SELECT
                DATE_FORMAT(o.orderDate, '%Y-%m') as month,
                SUM(o.totalPrice) as revenue
            FROM Order o
            WHERE o.orderDate BETWEEN :start AND :end
            GROUP BY DATE_FORMAT(o.orderDate, '%Y-%m')
            ORDER BY month
            """)
    List<Tuple> findMonthlyRevenue(LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT 
                u.userId,
                u.username as producerName,
                COUNT(DISTINCT CASE WHEN o.orderDate >= :start AND o.orderDate < :end THEN o.orderId END) as totalSales,
                COALESCE(SUM(CASE 
                    WHEN o.orderDate >= :start AND o.orderDate < :end THEN oi.price * oi.quantity 
                    ELSE 0 
                END), 0) as totalRevenue,
                CASE 
                    WHEN COALESCE(SUM(CASE 
                        WHEN o.orderDate >= DATE_SUB(:start, INTERVAL 1 MONTH) AND o.orderDate < :start THEN oi.price * oi.quantity 
                        ELSE 0 
                    END), 0) = 0 THEN 100.00
                    ELSE (
                        (COALESCE(SUM(CASE 
                            WHEN o.orderDate >= :start AND o.orderDate < :end THEN oi.price * oi.quantity 
                            ELSE 0 
                        END), 0) 
                        - COALESCE(SUM(CASE 
                            WHEN o.orderDate >= DATE_SUB(:start, INTERVAL 1 MONTH) AND o.orderDate < :start THEN oi.price * oi.quantity 
                            ELSE 0 
                        END), 0)) 
                        / COALESCE(SUM(CASE 
                            WHEN o.orderDate >= DATE_SUB(:start, INTERVAL 1 MONTH) AND o.orderDate < :start THEN oi.price * oi.quantity 
                            ELSE 0 
                        END), 1) * 100
                    )
                END as growthRate
            FROM User u
            LEFT JOIN Product p ON u.userId = p.producerId
            LEFT JOIN OrderItem oi ON p.productId = oi.productId
            LEFT JOIN `Order` o ON oi.orderId = o.orderId
            WHERE u.role = 'PRODUCER'
            GROUP BY u.userId, u.username
            ORDER BY totalRevenue DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Tuple> findProducerPerformance(LocalDateTime start, LocalDateTime end);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

   

    // Find orders by producer ID with pagination
    @Query(value = """
            SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId
            """)
    Page<Order> findByItemsProductProducerUserId(@Param("producerId") Long producerId, Pageable pageable);
    
    // Find orders by producer ID (no pagination)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p WHERE p.producer.userId = :producerId")
    List<Order> findByItemsProductProducerUserId(@Param("producerId") Long producerId);
    
    // Find orders by producer ID and status with pagination
    @Query(value = """
            SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND o.status = :status
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND o.status = :status
            """)
    Page<Order> findByItemsProductProducerUserIdAndStatus(
        @Param("producerId") Long producerId, 
        @Param("status") OrderStatus status, 
        Pageable pageable);
    
    // Find orders by producer ID and status (no pagination)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p WHERE p.producer.userId = :producerId AND o.status = :status")
    List<Order> findByItemsProductProducerUserIdAndStatus(@Param("producerId") Long producerId, @Param("status") OrderStatus status);

    // Count total orders
    @Query("SELECT COUNT(o) FROM Order o")
    int countAllOrders();

    // Count orders in a specific period
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    int countAllOrdersPreviousPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count total revenue
    @Query("SELECT SUM(o.totalPrice) FROM Order o")
    Double calculateTotalRevenue();

    // Count total revenue in a period
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    Double calculateTotalRevenuePreviousPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count orders by status (e.g., PENDING, DELIVERED, PROCESSING)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    int countOrdersByStatus(@Param("status") OrderStatus status);

    // Count orders in a specific period and status
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate AND o.status = :status")
    int countOrdersByStatusInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("status") OrderStatus status);

    // Revenue trend per month (monthly breakdown for current year)
    @Query("SELECT MONTH(o.orderDate) AS month, SUM(o.totalPrice) AS revenue FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate GROUP BY MONTH(o.orderDate) ORDER BY month")
    List<Object[]> calculateRevenueTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Monthly orders (count per month for current year)
    @Query("SELECT MONTH(o.orderDate) AS month, COUNT(o) AS totalOrders FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate GROUP BY MONTH(o.orderDate) ORDER BY month")
    List<Object[]> calculateMonthlyOrders(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    List<Order> findByGuestEmail(String guestEmail);

    Optional<Order> findByAccessToken(String accessToken);

    @Query("""
        SELECT COUNT(o) > 0 FROM Order o 
        JOIN o.items i 
        WHERE o.customer.userId = :customerId 
        AND i.product.productId = :productId 
        AND o.status = :status
    """)
    boolean existsByCustomerUserIdAndProductIdAndStatus(
        @Param("customerId") Long customerId, 
        @Param("productId") Long productId, 
        @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.accessToken = :accessToken")
    List<Order> findAllByAccessToken(@Param("accessToken") String accessToken);

    // Find all orders with a specific status
    List<Order> findByStatus(OrderStatus status);
    
    // Find the most recent order date for a customer and product
    @Query("SELECT MAX(o.orderDate) FROM Order o JOIN o.items i WHERE o.customer.userId = :customerId AND i.product.productId = :productId AND o.status = 'DELIVERED'")
    LocalDateTime findMostRecentOrderDateForCustomerAndProduct(@Param("customerId") Long customerId, @Param("productId") Long productId);

    // Find all order items for a specific order
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderId = :orderId")
    List<OrderItem> findOrderItemsByOrderId(@Param("orderId") Long orderId);

    // Find orders by producer ID and customer email containing (case insensitive)
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p " +
           "WHERE p.producer.userId = :producerId AND " +
           "LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    List<Order> findByProducerIdAndCustomerEmailContaining(
        @Param("producerId") Long producerId, 
        @Param("email") String email);
    
    // Find orders by producer ID and customer email containing with pagination
    @Query(value = """
            SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND 
            LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND 
            LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))
            """)
    Page<Order> findByProducerIdAndCustomerEmailContaining(
        @Param("producerId") Long producerId, 
        @Param("email") String email,
        Pageable pageable);
    
    // Find orders by producer ID, status, and customer email containing
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p " +
           "WHERE p.producer.userId = :producerId AND " +
           "o.status = :status AND " +
           "LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    List<Order> findByProducerIdAndStatusAndCustomerEmailContaining(
        @Param("producerId") Long producerId, 
        @Param("status") OrderStatus status,
        @Param("email") String email);
    
    // Find orders by producer ID, status, and customer email containing with pagination
    @Query(value = """
            SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND 
            o.status = :status AND 
            LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i JOIN i.product p 
            WHERE p.producer.userId = :producerId AND 
            o.status = :status AND 
            LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :email, '%'))
            """)
    Page<Order> findByProducerIdAndStatusAndCustomerEmailContaining(
        @Param("producerId") Long producerId, 
        @Param("status") OrderStatus status,
        @Param("email") String email,
        Pageable pageable);
}
