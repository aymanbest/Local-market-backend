package com.localmarket.main.repository.product;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.product.StockReservation;
import com.localmarket.main.entity.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByProductAndExpiresAtGreaterThan(Product product, LocalDateTime now);
    List<StockReservation> findByExpiresAtLessThan(LocalDateTime now);
    void deleteByOrder(Order order);
} 