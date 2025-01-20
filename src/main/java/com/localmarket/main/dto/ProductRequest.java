package com.localmarket.main.dto;
import java.math.BigDecimal;
import java.util.Set;
import lombok.Data;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private Set<Long> categoryIds;
} 