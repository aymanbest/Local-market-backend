package com.localmarket.main.entity.review;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;


@Entity
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewId")
    private Long reviewId;

    @ManyToOne
    @JoinColumn(name = "productId")
    private Product product;


    @ManyToOne
    @JoinColumn(name = "customerId")
    private User customer;

    @Column(nullable = false)
    @NotNull
    @Min(0)
    @Max(5)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "LONGTEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ReviewStatus status = ReviewStatus.PENDING;


    @Column(name = "verifiedPurchase")
    private boolean verifiedPurchase;

    @Column(name = "createdAt")
    private LocalDateTime createdAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 