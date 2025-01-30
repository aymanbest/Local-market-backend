package com.localmarket.main.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.dto.user.FilterUsersResponse;
import com.localmarket.main.dto.review.VerifiedReviews;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private FilterUsersResponse producer;
    private Set<Category> categories;
    private List<VerifiedReviews> verifiedReviews;
}

