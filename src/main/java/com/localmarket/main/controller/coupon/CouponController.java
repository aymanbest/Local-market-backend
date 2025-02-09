package com.localmarket.main.controller.coupon;

import com.localmarket.main.entity.coupon.Coupon;
import com.localmarket.main.service.coupon.CouponService;
import com.localmarket.main.dto.coupon.CouponRequest;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.dto.coupon.CouponStatsResponse;
import com.localmarket.main.dto.coupon.CouponValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create coupon", description = "Create a new coupon (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Coupon> createCoupon(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.createCoupon(request));
    }

    @PutMapping("/{couponId}")
    @AdminOnly
    @Operation(summary = "Update coupon", description = "Update an existing coupon (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(couponId, request));
    }

    @DeleteMapping("/{couponId}")
    @AdminOnly
    @Operation(summary = "Delete coupon", description = "Delete an existing coupon (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @AdminOnly
    @Operation(summary = "Get all coupons", description = "Get all coupons with usage statistics (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<List<CouponStatsResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCouponsWithStats());
    }

    @GetMapping("/validate/{code}")
    @Operation(summary = "Validate coupon", description = "Check if a coupon exists and is valid")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @PathVariable String code,
            @RequestParam BigDecimal cartTotal) {
        return ResponseEntity.ok(couponService.validateCoupon(code, cartTotal));
    }
} 