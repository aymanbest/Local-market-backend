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
    @Column(name = "couponId")
    private Long couponId;
    

    // The coupon code users will enter (e.g., "GIGD2025")
    @Column(name = "code",unique = true)
    private String code;
    
    @Column(name = "description")
    private String description;
    

    // Either "PERCENTAGE" (%) or "FIXED_AMOUNT" ($)
    @Enumerated(EnumType.STRING)
    @Column(name = "discountType")
    private DiscountType discountType;
    

    // For PERCENTAGE: represents percentage (20.00 = 20%)
    // For FIXED_AMOUNT: represents dollar amount ($10.00)
    @Column(name = "discountValue")
    private BigDecimal discountValue;
    
    // Minimum order total required to use the coupon
    @Column(name = "minimumPurchaseAmount")
    private BigDecimal minimumPurchaseAmount;
    
    // Maximum discount amount allowed (prevents high % discounts on expensive orders)
    @Column(name = "maximumDiscountAmount")
    private BigDecimal maximumDiscountAmount;
    
    // Date range when the coupon is valid
    @Column(name = "validFrom")
    private LocalDateTime validFrom;
    
    @Column(name = "validUntil")
    private LocalDateTime validUntil;
    
    // Maximum number of times the coupon can be used
    @Column(name = "usageLimit")
    private Integer usageLimit;
    
    // How many times the coupon has been used
    @Column(name = "timesUsed")
    private Integer timesUsed;


    // Whether the coupon is currently active
    @Column(name = "isActive")
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