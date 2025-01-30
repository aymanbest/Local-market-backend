package com.localmarket.main.dto.review;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ReviewEligibilityResponse {
    private boolean eligible;
    private boolean hasReviewed;
    private String message;
} 