package com.localmarket.main.dto.coupon;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.localmarket.main.entity.coupon.DiscountType;

@Data
@Builder
public class CouponStatsResponse {
    private Long couponId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private Integer timesUsed;
    private Boolean isActive;
    private Boolean isExpired;
    private Integer remainingUses;
} 