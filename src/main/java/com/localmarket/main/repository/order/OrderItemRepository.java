package com.localmarket.main.repository.order;

import com.localmarket.main.entity.order.OrderItem;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi")
    Integer countTotalProductsSoldAllTime();

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.order.orderDate BETWEEN :startDate AND :endDate")
    Integer countTotalProductsSoldInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
