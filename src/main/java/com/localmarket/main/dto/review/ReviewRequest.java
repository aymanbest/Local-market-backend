package com.localmarket.main.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long productId;
    
    @NotNull
    @Min(0)
    @Max(5)
    private Integer rating;
    
    private String comment;
} 