package com.localmarket.main.entity.product;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.localmarket.main.entity.category.Category;
import com.localmarket.main.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "Product")
@EqualsAndHashCode(exclude = {"categories"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producerId")
    @JsonIgnoreProperties({"passwordHash", "createdAt", "updatedAt", "email"})
    private User producer;
    
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private String imageUrl;
    
    @ManyToMany(fetch = FetchType.LAZY)

    @JoinTable(
        name = "ProductCategory",
        joinColumns = @JoinColumn(name = "productId"),
        inverseJoinColumns = @JoinColumn(name = "categoryId")
    )
    
    @JsonIgnoreProperties("products")
    @ToString.Exclude
    private Set<Category> categories = new HashSet<>();
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 