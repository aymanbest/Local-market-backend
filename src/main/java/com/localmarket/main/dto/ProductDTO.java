package com.localmarket.main.dto;

import com.localmarket.main.entity.Category;
import com.localmarket.main.entity.Role;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class ProductDTO {
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long producerId;
    private String producerUsername;
    private String producerEmail;
    private Role producerRole;
    private Set<Category> categories;

    public ProductDTO() {
    }

    public ProductDTO(
            Long productId, 
            String name, 
            String description, 
            BigDecimal price,
            Integer quantity, 
            String imageUrl, 
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long producerId,
            String producerUsername,
            String producerEmail,
            Role producerRole) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.producerId = producerId;
        this.producerUsername = producerUsername;
        this.producerEmail = producerEmail;
        this.producerRole = producerRole;
    }

    public ProductDTO(
            Long productId, 
            String name, 
            String description, 
            BigDecimal price,
            Integer quantity, 
            String imageUrl, 
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long producerId,
            String producerUsername,
            String producerEmail,
            Role producerRole,
            Set<Category> categories) {
        this(productId, name, description, price, quantity, imageUrl, 
             createdAt, updatedAt, producerId, producerUsername, 
             producerEmail, producerRole);
        this.categories = categories;
    }
} 