package com.localmarket.main.repository.order;

import com.localmarket.main.entity.order.Order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.localmarket.main.entity.order.OrderStatus;

import java.util.List;
import java.util.Optional;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerUserId(Long userId);
    List<Order> findByCustomerUserIdAndStatus(Long userId, OrderStatus status);
    Optional<Order> findByOrderIdAndCustomerUserId(Long orderId, Long userId);
} 