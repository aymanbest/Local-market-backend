package com.localmarket.main.repository.review;

import com.localmarket.main.entity.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.localmarket.main.entity.review.ReviewStatus;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByCustomerUserId(Long customerId);
    
    Optional<Review> findByProductProductIdAndCustomerUserId(Long productId, Long customerId);
    
    List<Review> findByProductProductId(Long productId);
    
    @Query("""
        SELECT COUNT(r) > 0 FROM Review r 
        WHERE r.product.productId = :productId 
        AND r.customer.userId = :customerId
    """)
    boolean existsByProductAndCustomer(@Param("productId") Long productId, 
                                     @Param("customerId") Long customerId);

    List<Review> findByStatus(ReviewStatus status);
} 