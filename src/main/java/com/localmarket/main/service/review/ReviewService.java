package com.localmarket.main.service.review;

import com.localmarket.main.entity.review.Review;
import com.localmarket.main.repository.review.ReviewRepository;
import com.localmarket.main.repository.order.OrderRepository;
import com.localmarket.main.repository.product.ProductRepository;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.dto.review.*;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.entity.order.OrderStatus;
import com.localmarket.main.entity.review.ReviewStatus;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import com.localmarket.main.service.notification.review.ReviewNotificationService;
import com.localmarket.main.service.notification.admin.AdminNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ReviewNotificationService reviewNotificationService;
    private final AdminNotificationService adminNotificationService;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReviewService.class);
    
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkReviewEligibility(Long productId, Long customerId) {
        boolean hasOrdered = orderRepository.existsByCustomerUserIdAndProductIdAndStatus(
            customerId, productId, OrderStatus.DELIVERED);
            
        boolean hasReviewed = reviewRepository.existsByProductAndCustomer(productId, customerId);
        
        if (!hasOrdered) {
            return ReviewEligibilityResponse.builder()
                .eligible(false)
                .hasReviewed(false)
                .message("You must purchase and receive this product before reviewing it")
                .build();
        }
        
        if (hasReviewed) {
            return ReviewEligibilityResponse.builder()
                .eligible(true)
                .hasReviewed(true)
                .message("You have already reviewed this product")
                .build();
        }
        
        return ReviewEligibilityResponse.builder()
            .eligible(true)
            .hasReviewed(false)
            .message("You can review this product")
            .build();
    }
    
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long customerId) {
        try {
            // First check if product exists
            Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ApiException(ErrorType.PRODUCT_NOT_FOUND, 
                    "Product with ID " + request.getProductId() + " not found"));
                
            // Check if user exists
            User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, 
                    "User not found"));

            // Check if user has purchased and received the product
            boolean hasOrdered = orderRepository.existsByCustomerUserIdAndProductIdAndStatus(
                customerId, request.getProductId(), OrderStatus.DELIVERED);
            
            if (!hasOrdered) {
                throw new ApiException(ErrorType.INVALID_REQUEST, 
                    "You must purchase and receive this product before reviewing it");
            }
            
            // Check if user has already reviewed this product
            boolean hasReviewed = reviewRepository.existsByProductAndCustomer(
                request.getProductId(), customerId);
            
            if (hasReviewed) {
                throw new ApiException(ErrorType.INVALID_REQUEST, 
                    "You have already reviewed this product");
            }

            Review review = new Review();
            review.setProduct(product);
            review.setCustomer(customer);
            review.setRating(request.getRating());
            review.setComment(request.getComment());
            review.setStatus(ReviewStatus.PENDING);
            review.setVerifiedPurchase(true);

            Review savedReview = reviewRepository.save(review);

            adminNotificationService.notifyNewReviewNeedsApproval(review);

            return convertToDTO(savedReview);
        } catch (Exception e) {
            log.error("Error creating review", e);
            throw e;
        }
    }
    
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, Long customerId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ApiException(ErrorType.REVIEW_NOT_FOUND, 
                "Review not found"));
                
        if (!review.getCustomer().getUserId().equals(customerId)) {
            throw new ApiException(ErrorType.REVIEW_ACCESS_DENIED, 
                "You can only edit your own reviews");
        }
        
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(ReviewStatus.PENDING); // Reset to pending for admin approval
        
        return convertToDTO(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse approveReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ApiException(ErrorType.REVIEW_NOT_FOUND, "Review not found"));
        
        review.setStatus(ReviewStatus.APPROVED);
        review = reviewRepository.save(review);
        reviewNotificationService.notifyReviewStatusUpdate(review);
        return convertToDTO(review);
    }

    @Transactional
    public ReviewResponse declineReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new ApiException(ErrorType.REVIEW_NOT_FOUND, "Review not found"));
        
        review.setStatus(ReviewStatus.DECLINED);
        review = reviewRepository.save(review);
        reviewNotificationService.notifyReviewStatusUpdate(review);
        return convertToDTO(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getCustomerReviews(Long customerId, Pageable pageable) {
        List<Review> reviews = reviewRepository.findByCustomerUserId(customerId);
        
        // Sort reviews by the requested field
        List<Review> sortedReviews = reviews.stream()
            .sorted((r1, r2) -> {
                if (pageable.getSort().isEmpty()) {
                    return 0;
                }
                String sortBy = pageable.getSort().iterator().next().getProperty();
                boolean isAsc = pageable.getSort().iterator().next().isAscending();
                
                int comparison = switch(sortBy) {
                    case "rating" -> Integer.compare(r1.getRating(), r2.getRating());
                    case "createdAt" -> r1.getCreatedAt().compareTo(r2.getCreatedAt());
                    case "status" -> r1.getStatus().compareTo(r2.getStatus());
                    case "productName" -> r1.getProduct().getName().compareTo(r2.getProduct().getName());
                    default -> 0;
                };
                return isAsc ? comparison : -comparison;
            })
            .collect(Collectors.toList());
            
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedReviews.size());
        
        if (start >= sortedReviews.size()) {
            return new PageImpl<>(List.of(), pageable, sortedReviews.size());
        }
        
        List<Review> paginatedReviews = sortedReviews.subList(start, end);
        
        return new PageImpl<>(
            paginatedReviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()),
            pageable,
            sortedReviews.size()
        );
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        return reviewRepository.findByProductProductId(productId)
            .stream()
            .filter(review -> review.getStatus() == ReviewStatus.APPROVED)
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByStatus(ReviewStatus.PENDING)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private ReviewResponse convertToDTO(Review review) {
        return ReviewResponse.builder()
            .reviewId(review.getReviewId())
            .productId(review.getProduct().getProductId())
            .productName(review.getProduct().getName())
            .customerUsername(review.getCustomer().getUsername())
            .rating(review.getRating())
            .comment(review.getComment())
            .status(review.getStatus())
            .verifiedPurchase(review.isVerifiedPurchase())
            .createdAt(review.getCreatedAt())
            .build();
    }
} 