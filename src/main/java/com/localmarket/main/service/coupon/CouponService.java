package com.localmarket.main.service.coupon;

import com.localmarket.main.entity.coupon.Coupon;
import com.localmarket.main.entity.coupon.UserCouponUsage;
import com.localmarket.main.repository.coupon.CouponRepository;
import com.localmarket.main.repository.coupon.UserCouponUsageRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;
import com.localmarket.main.dto.coupon.CouponStatsResponse;
import com.localmarket.main.dto.coupon.CouponValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;
    private final UserCouponUsageRepository userCouponUsageRepository;
    private static final String WELCOME_COUPON_CODE = "WELCOME10";

    @Transactional
    public void initializeWelcomeCoupon() {
        if (!couponRepository.findByCode(WELCOME_COUPON_CODE).isPresent()) {
            Coupon welcomeCoupon = new Coupon();
            welcomeCoupon.setCode(WELCOME_COUPON_CODE);
            welcomeCoupon.setDescription("10% off for new users");
            welcomeCoupon.setDiscountType(DiscountType.PERCENTAGE);
            welcomeCoupon.setDiscountValue(new BigDecimal("10.00"));
            welcomeCoupon.setMinimumPurchaseAmount(new BigDecimal("0.00"));
            welcomeCoupon.setValidFrom(LocalDateTime.now());
            welcomeCoupon.setValidUntil(LocalDateTime.now().plusYears(1));
            welcomeCoupon.setIsActive(true);
            welcomeCoupon.setTimesUsed(0);
            
            couponRepository.save(welcomeCoupon);
        }
    }

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
    public void applyCoupon(String couponCode, Long userId) {
        Coupon coupon = validateAndGetCoupon(couponCode);
        
        if (userCouponUsageRepository.existsByUserIdAndCoupon_Code(userId, couponCode)) {
            throw new ApiException(ErrorType.VALIDATION_FAILED, "You have already used this coupon");
        }

        UserCouponUsage usage = UserCouponUsage.builder()
            .userId(userId)
            .coupon(coupon)
            .build();
        
        userCouponUsageRepository.save(usage);
        
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

    @Transactional(readOnly = true)
    public Page<CouponStatsResponse> getAllCouponsWithStats(Pageable pageable) {
        List<Coupon> coupons = couponRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        
        // Sort coupons
        List<Coupon> sortedCoupons = coupons.stream()
            .sorted((c1, c2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "code" -> c1.getCode().compareTo(c2.getCode());
                    case "isActive" -> Boolean.compare(c1.getIsActive(), c2.getIsActive());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedCoupons.size());
        
        if (start >= sortedCoupons.size()) {
            return new PageImpl<>(List.of(), pageable, sortedCoupons.size());
        }
        
        List<Coupon> paginatedCoupons = sortedCoupons.subList(start, end);
        
        List<CouponStatsResponse> responses = paginatedCoupons.stream()
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
        
        return new PageImpl<>(responses, pageable, sortedCoupons.size());
    }

    public CouponValidationResponse validateCoupon(String code, BigDecimal cartTotal, Long userId) {
        try {
            Coupon coupon = validateAndGetCoupon(code);
            
            if (userCouponUsageRepository.existsByUserIdAndCoupon_Code(userId, code)) {
                return CouponValidationResponse.builder()
                    .valid(false)
                    .message("You have already used this coupon")
                    .build();
            }
            
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

    public Optional<Coupon> checkUserWelcomeCoupon(Long userId) {
        if (userCouponUsageRepository.existsByUserIdAndCoupon_Code(userId, WELCOME_COUPON_CODE)) {
            return Optional.empty();
        }
        return couponRepository.findByCode(WELCOME_COUPON_CODE);
    }

    @Transactional
    public Coupon updateCouponStatus(Long couponId, Boolean isActive) {
        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Coupon not found"));
            
        coupon.setIsActive(isActive);
        return couponRepository.save(coupon);
    }
} 