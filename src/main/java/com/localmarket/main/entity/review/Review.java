package com.localmarket.main.entity.review;

import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "Review")
@Data @Getter
@EqualsAndHashCode(exclude = {"customer", "product"})
@ToString
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewId")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "productId")
    @JsonIdentityReference(alwaysAsId = true)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerId")
    @JsonBackReference
    @ToString.Exclude
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