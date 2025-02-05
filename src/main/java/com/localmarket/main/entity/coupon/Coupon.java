package com.localmarket.main.entity.coupon;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long couponId;
    
    // The coupon code users will enter (e.g., "GIGD2025")
    @Column(unique = true)
    private String code;
    
    private String description;
    
    // Either "PERCENTAGE" (%) or "FIXED_AMOUNT" ($)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;
    
    // For PERCENTAGE: represents percentage (20.00 = 20%)
    // For FIXED_AMOUNT: represents dollar amount ($10.00)
    private BigDecimal discountValue;
    
    // Minimum order total required to use the coupon
    private BigDecimal minimumPurchaseAmount;
    
    // Maximum discount amount allowed (prevents high % discounts on expensive orders)
    private BigDecimal maximumDiscountAmount;
    
    // Date range when the coupon is valid
    private LocalDateTime validFrom;
    
    private LocalDateTime validUntil;
    
    // Maximum number of times the coupon can be used
    private Integer usageLimit;
    
    // How many times the coupon has been used
    private Integer timesUsed;
    
    // Whether the coupon is currently active
    private Boolean isActive;
    
    @PrePersist
    protected void onCreate() {
        if (timesUsed == null) {
            timesUsed = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }
} 