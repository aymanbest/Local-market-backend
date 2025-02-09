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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Entity
@Data
@Table(name = "Product")
@EqualsAndHashCode(exclude = {"categories"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "productId")
    private Long productId;
    
    @NotNull(message = "Producer is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producerId")
    @JsonIgnoreProperties({"passwordHash", "createdAt", "updatedAt", "email"})
    private User producer;
    
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Column(name = "name")
    private String name;


    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(name = "description")
    private String description;
    

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    @Column(name = "price")
    private BigDecimal price;
    

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(name = "quantity")
    private Integer quantity;
    

    @Column(name = "imageUrl")
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
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ProductStatus status = ProductStatus.PENDING;

    @Column(name = "declineReason")
    private String declineReason;
    
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