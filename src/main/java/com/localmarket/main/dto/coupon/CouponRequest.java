package com.localmarket.main.dto.coupon;

import lombok.Data;
import com.localmarket.main.entity.coupon.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponRequest {
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumPurchaseAmount;
    private BigDecimal maximumDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer usageLimit;
    private Boolean isActive;
} 