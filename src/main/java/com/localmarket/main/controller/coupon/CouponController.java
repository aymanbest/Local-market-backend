package com.localmarket.main.controller.coupon;

import com.localmarket.main.entity.coupon.Coupon;
import com.localmarket.main.service.coupon.CouponService;
import com.localmarket.main.dto.coupon.CouponRequest;
import com.localmarket.main.security.AdminOnly;
import com.localmarket.main.dto.coupon.CouponStatsResponse;
import com.localmarket.main.dto.coupon.CouponValidationResponse;
import com.localmarket.main.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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

    @PostMapping("/initialize-welcome")
    @AdminOnly
    @Operation(summary = "Initialize welcome coupon", description = "Create the default 10% welcome coupon if it doesn't exist (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Void> initializeWelcomeCoupon() {
        couponService.initializeWelcomeCoupon();
        return ResponseEntity.ok().build();
    }

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
    public ResponseEntity<Page<CouponStatsResponse>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(couponService.getAllCouponsWithStats(pageable));
    }

    @GetMapping("/validate/{code}")
    @Operation(summary = "Validate coupon", description = "Check if a coupon exists and is valid for the current user")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @PathVariable String code,
            @RequestParam BigDecimal cartTotal,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(couponService.validateCoupon(code, cartTotal, userDetails.getId()));
    }

    @GetMapping("/check-welcome")
    @Operation(summary = "Check welcome coupon", description = "Check if the user is eligible for the welcome coupon")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Coupon> checkWelcomeCoupon(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return couponService.checkUserWelcomeCoupon(userDetails.getId())
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{couponId}/status")
    @AdminOnly
    @Operation(summary = "Update coupon status", description = "Enable or disable a coupon (Admin only)")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Coupon> updateCouponStatus(
            @PathVariable Long couponId,
            @RequestParam Boolean isActive) {
        return ResponseEntity.ok(couponService.updateCouponStatus(couponId, isActive));
    }
} 