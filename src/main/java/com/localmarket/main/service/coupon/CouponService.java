package com.localmarket.main.service.coupon;

import com.localmarket.main.entity.coupon.Coupon;
import com.localmarket.main.repository.coupon.CouponRepository;
import com.localmarket.main.dto.coupon.CouponRequest;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.entity.coupon.DiscountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.localmarket.main.dto.coupon.CouponStatsResponse;
import com.localmarket.main.dto.coupon.CouponValidationResponse;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;

    @Transactional
    public Coupon createCoupon(CouponRequest request) {
        validateCouponRequest(request);
        
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Coupon code already exists");
        }

        Coupon coupon = new Coupon();
        mapRequestToCoupon(request, coupon);
        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon updateCoupon(Long couponId, CouponRequest request) {
        validateCouponRequest(request);
        
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Coupon not found"));
            
        mapRequestToCoupon(request, coupon);
        return couponRepository.save(coupon);
    }

    public BigDecimal calculateDiscount(String couponCode, BigDecimal orderAmount) {
        Coupon coupon = validateAndGetCoupon(couponCode);
        
        if (orderAmount.compareTo(coupon.getMinimumPurchaseAmount()) < 0) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, 
                "Order amount does not meet minimum purchase requirement");
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue().divide(new BigDecimal("100")));
        } else {
            discount = coupon.getDiscountValue();
        }

        // Apply maximum discount limit if set
        if (coupon.getMaximumDiscountAmount() != null) {
            discount = discount.min(coupon.getMaximumDiscountAmount());
        }

        return discount;
    }

    @Transactional
    public void applyCoupon(String couponCode) {
        Coupon coupon = validateAndGetCoupon(couponCode);
        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Coupon not found"));
            
        // Check if coupon has been used
        if (coupon.getTimesUsed() > 0) {
            // Soft delete by deactivating instead of removing
            coupon.setIsActive(false);
            couponRepository.save(coupon);
        } else {
            // Hard delete if never used
            couponRepository.deleteById(couponId);
        }
    }

    private Coupon validateAndGetCoupon(String couponCode) {
        Coupon coupon = couponRepository.findByCode(couponCode)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Invalid coupon code"));

        LocalDateTime now = LocalDateTime.now();
        
        if (!coupon.getIsActive()) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Coupon is inactive");
        }
        
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Coupon is expired or not yet valid");
        }
        
        if (coupon.getUsageLimit() != null && coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Coupon usage limit exceeded");
        }

        return coupon;
    }

    private void validateCouponRequest(CouponRequest request) {
        if (request.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Discount value must be greater than 0");
        }
        
        if (request.getDiscountType() == DiscountType.PERCENTAGE && 
            request.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "Percentage discount cannot exceed 100%");
        }
    }

    private void mapRequestToCoupon(CouponRequest request, Coupon coupon) {
        coupon.setCode(request.getCode());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumPurchaseAmount(request.getMinimumPurchaseAmount());
        coupon.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setIsActive(request.getIsActive());
    }

    public List<CouponStatsResponse> getAllCouponsWithStats() {
        List<Coupon> coupons = couponRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        return coupons.stream()
            .map(coupon -> CouponStatsResponse.builder()
                .couponId(coupon.getCouponId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minimumPurchaseAmount(coupon.getMinimumPurchaseAmount())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .timesUsed(coupon.getTimesUsed())
                .isActive(coupon.getIsActive())
                .isExpired(now.isAfter(coupon.getValidUntil()))
                .remainingUses(coupon.getUsageLimit() != null ? 
                    Math.max(0, coupon.getUsageLimit() - coupon.getTimesUsed()) : null)
                .build())
            .collect(Collectors.toList());
    }

    public CouponValidationResponse validateCoupon(String code, BigDecimal cartTotal) {
        try {
            Coupon coupon = validateAndGetCoupon(code);
            
            if (cartTotal.compareTo(coupon.getMinimumPurchaseAmount()) < 0) {
                return CouponValidationResponse.builder()
                    .valid(false)
                    .message("Order total must be at least $" + coupon.getMinimumPurchaseAmount())
                    .build();
            }

            BigDecimal discount = calculateDiscount(code, cartTotal);
            BigDecimal finalPrice = cartTotal.subtract(discount);
            
            String description = coupon.getDiscountType() == DiscountType.PERCENTAGE ?
                coupon.getDiscountValue() + "% off" :
                "$" + coupon.getDiscountValue() + " off";
                
            if (coupon.getMinimumPurchaseAmount() != null) {
                description += " on orders over $" + coupon.getMinimumPurchaseAmount();
            }

            return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied successfully")
                .discountAmount(discount)
                .finalPrice(finalPrice)
                .discountDescription(description)
                .build();
                
        } catch (ApiException e) {
            return CouponValidationResponse.builder()
                .valid(false)
                .message(e.getMessage())
                .build();
        }
    }
} 