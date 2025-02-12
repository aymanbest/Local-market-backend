package com.localmarket.main.controller.review;

import com.localmarket.main.dto.review.*;
import com.localmarket.main.service.review.ReviewService;
import com.localmarket.main.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.localmarket.main.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review management APIs")
public class ReviewController {
    private final ReviewService reviewService;
    
    @GetMapping("/eligibility/{productId}")
    @Operation(summary = "Check review eligibility", description = "Check if user can review a product")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<ReviewEligibilityResponse> checkEligibility(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reviewService.checkReviewEligibility(productId, userDetails.getId()));
    }

    @PostMapping
    @Operation(summary = "Create review", description = "Create a new review for a product")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reviewService.createReview(request, userDetails.getId()));
    }

    @PutMapping("/{reviewId}")
    @Operation(summary = "Update review", description = "Update an existing review")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, userDetails.getId()));
    }

    @GetMapping
    @Operation(summary = "Get customer reviews", description = "Get all reviews by the authenticated customer with pagination")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Page<ReviewResponse>> getCustomerReviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(reviewService.getCustomerReviews(userDetails.getId(), pageable));
    }

    @PostMapping("/{reviewId}/approve")
    @Operation(summary = "Approve review", description = "Approve a pending review")
    @SecurityRequirement(name = "cookie")
    @AdminOnly
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.approveReview(reviewId));
    }

    @PostMapping("/{reviewId}/decline")
    @Operation(summary = "Decline review", description = "Decline a pending review")
    @SecurityRequirement(name = "cookie")
    @AdminOnly
    public ResponseEntity<ReviewResponse> declineReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.declineReview(reviewId));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all approved reviews for a product")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending reviews", description = "Get all pending reviews (Admin only)")
    @SecurityRequirement(name = "cookie")
    @AdminOnly
    public ResponseEntity<List<ReviewResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }
} 