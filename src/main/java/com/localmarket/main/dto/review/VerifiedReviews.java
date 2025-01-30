package com.localmarket.main.dto.review;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class VerifiedReviews {
    private Long reviewId;
    private String customerUsername;
    private Integer rating;
    private String comment;
    private boolean verifiedPurchase;
    private LocalDateTime createdAt;
}
