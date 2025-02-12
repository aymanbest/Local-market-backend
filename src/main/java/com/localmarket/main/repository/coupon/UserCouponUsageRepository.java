package com.localmarket.main.repository.coupon;

import com.localmarket.main.entity.coupon.UserCouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserCouponUsageRepository extends JpaRepository<UserCouponUsage, Long> {
    boolean existsByUserIdAndCoupon_Code(Long userId, String couponCode);
    Optional<UserCouponUsage> findByUserIdAndCoupon_Code(Long userId, String couponCode);
} 