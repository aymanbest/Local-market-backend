package com.localmarket.main.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import com.localmarket.main.entity.category.Category;
import com.localmarket.main.entity.product.ProductStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyProductResponse {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<Category> categories;
    private ProductStatus status;
    private String declineReason;
    private boolean stock;
} 