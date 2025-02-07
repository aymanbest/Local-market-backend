package com.localmarket.main.controller.review;

import com.localmarket.main.dto.review.*;
import com.localmarket.main.service.review.ReviewService;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.localmarket.main.util.CookieUtil;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review management APIs")
public class ReviewController {
    private final ReviewService reviewService;
    private final JwtService jwtService;
    private final CookieUtil cookieUtil;
    
    @GetMapping("/eligibility/{productId}")
    @Operation(summary = "Check review eligibility", description = "Check if user can review a product")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ReviewEligibilityResponse> checkEligibility(
            @PathVariable Long productId,
            HttpServletRequest request) {
        String jwt = cookieUtil.getJwtFromRequest(request);     
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(reviewService.checkReviewEligibility(productId, customerId));
    }

    
    @PostMapping
    @Operation(summary = "Create review", description = "Create a new review for a product")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody ReviewRequest request,
            HttpServletRequest requestco) {
        String jwt = cookieUtil.getJwtFromRequest(requestco);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(reviewService.createReview(request, customerId));
    }

    
    @PutMapping("/{reviewId}")
    @Operation(summary = "Update review", description = "Update an existing review")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest request,
            HttpServletRequest requestco) {
        String jwt = cookieUtil.getJwtFromRequest(requestco);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, customerId));
    }



    @PostMapping("/{reviewId}/approve")
    @Operation(summary = "Approve review", description = "Approve a pending review")
    @SecurityRequirement(name = "bearer-jwt")
    @AdminOnly
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.approveReview(reviewId));
    }

    @PostMapping("/{reviewId}/decline")
    @Operation(summary = "Decline review", description = "Decline a pending review")
    @SecurityRequirement(name = "bearer-jwt")
    @AdminOnly
    public ResponseEntity<ReviewResponse> declineReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.declineReview(reviewId));
    }

    @GetMapping
    @Operation(summary = "Get customer reviews", description = "Get all reviews by the authenticated customer")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<ReviewResponse>> getCustomerReviews(
            HttpServletRequest requestco) {
        String jwt = cookieUtil.getJwtFromRequest(requestco);
        Long customerId = jwtService.extractUserId(jwt);
        return ResponseEntity.ok(reviewService.getCustomerReviews(customerId));
    }


    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all approved reviews for a product")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending reviews", description = "Get all pending reviews (Admin only)")
    @SecurityRequirement(name = "bearer-jwt")
    @AdminOnly
    public ResponseEntity<List<ReviewResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }
} 