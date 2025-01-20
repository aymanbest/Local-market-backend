package com.localmarket.main.repository.order;

import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.localmarket.main.entity.order.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(User customer);
    Optional<Order> findByOrderIdAndGuestEmail(Long orderId, String guestEmail);
    List<Order> findByCustomerAndStatus(User customer, OrderStatus status);
    Optional<Order> findByOrderIdAndCustomer(Long orderId, User customer);
} 