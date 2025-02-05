package com.localmarket.main.repository.coupon;

import com.localmarket.main.entity.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);
    
    List<Coupon> findByIsActiveAndValidFromBeforeAndValidUntilAfter(
        Boolean isActive, 
        LocalDateTime now, 
        LocalDateTime now2
    );
} 