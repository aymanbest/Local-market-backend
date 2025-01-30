package com.localmarket.main.dto.review;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.Builder;
import com.localmarket.main.entity.review.ReviewStatus;

@Data
@Builder
public class ReviewResponse {
    private Long reviewId;
    private Long productId;
    private String productName;
    private String customerUsername;
    private Integer rating;
    private String comment;
    private ReviewStatus status;
    private boolean verifiedPurchase;
    private LocalDateTime createdAt;
} 