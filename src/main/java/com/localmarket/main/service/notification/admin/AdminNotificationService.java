package com.localmarket.main.service.notification.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.localmarket.main.service.notification.WebSocketService;
import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.entity.producer.ProducerApplication;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.review.Review;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import com.localmarket.main.entity.user.Role;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {
    private final WebSocketService webSocketService;

    public void notifyNewProducerApplication(ProducerApplication application) {
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", application.getApplicationId());
        data.put("businessName", application.getBusinessName());
        data.put("customerEmail", application.getCustomer().getEmail());
        data.put("timestamp", application.getCreatedAt());
        
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_PRODUCER_APPLICATION")
            .message("New producer application from: " + application.getBusinessName())
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole(Role.ADMIN.name(), notification);
    }

    public void notifyNewProductNeedsReview(Product product) {
        System.err.println("New product needs review: " + product.getName() + " " + product.getProductId()
         + " " + product.getCategories() + " " + product.getProducer().getUsername() + " " + product.getProducer().getEmail());
        
        Map<String, Object> data = new HashMap<>();
        data.put("productId", product.getProductId());
        data.put("productName", product.getName());
        data.put("categories", product.getCategories());
        
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_PRODUCT_NEED_REVIEW")
            .message("New product needs review: " + product.getName())
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole(Role.ADMIN.name(), notification);
    }

    public void notifyNewReviewNeedsApproval(Review review) {
        Map<String, Object> data = new HashMap<>();
        data.put("reviewId", review.getReviewId());
        data.put("productName", review.getProduct().getName());
        data.put("customerUsername", review.getCustomer().getUsername());
        data.put("rating", review.getRating());
        data.put("comment", review.getComment());
        
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_REVIEW_NEED_REVIEW")
            .message("New review needs approval for product: " + review.getProduct().getName())
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole(Role.ADMIN.name(), notification);
    }
} 