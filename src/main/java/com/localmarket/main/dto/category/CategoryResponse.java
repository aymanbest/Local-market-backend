package com.localmarket.main.dto.category;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class CategoryResponse {
    private Long categoryId;
    private String name;
    private Integer productCount;
} 