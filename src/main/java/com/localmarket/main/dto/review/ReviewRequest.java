package com.localmarket.main.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long productId;
    
    @NotNull
    @Min(0)
    @Max(5)
    private Integer rating;
    
    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;
} 