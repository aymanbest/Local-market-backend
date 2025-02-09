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

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {
    private final WebSocketService webSocketService;

    public void notifyNewProducerApplication(ProducerApplication application) {
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_PRODUCER_APPLICATION")
            .message("New producer application received from " + application.getBusinessName())
            .data(Map.of(
                "applicationId", application.getApplicationId(),
                "businessName", application.getBusinessName(),
                "customerEmail", application.getCustomer().getEmail(),
                "customerUsername", application.getCustomer().getUsername(),
                "categories", application.getCategoryIds(),
                "customCategory", application.getCustomCategory()
            ))
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole("ADMIN", notification);
    }

    public void notifyNewProductNeedsReview(Product product) {
        System.err.println("New product needs review: " + product.getName() + " " + product.getProductId()
         + " " + product.getCategories() + " " + product.getProducer().getUsername() + " " + product.getProducer().getEmail());
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_PRODUCT_NEED_REVIEW")
            .message("New product needs review: " + product.getName())
            .data(Map.of(
                "productId", product.getProductId(),
                "productName", product.getName(),
                "categories", product.getCategories()
            ))
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole("ADMIN", notification);
    }

    public void notifyNewReviewNeedsApproval(Review review) {
        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_REVIEW_NEED_REVIEW")
            .message("New review needs approval for product: " + review.getProduct().getName())
            .data(Map.of(
                "reviewId", review.getReviewId(),
                "productName", review.getProduct().getName(),
                "customerUsername", review.getCustomer().getUsername(),
                "rating", review.getRating(),
                "comment", review.getComment()
            ))
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToRole("ADMIN", notification);
    }
} 