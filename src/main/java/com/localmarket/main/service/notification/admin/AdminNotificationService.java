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
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.websocket.NotificationWebSocketHandler;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {
    private final WebSocketService webSocketService;
    private final NotificationWebSocketHandler webSocketHandler;

    public void notifyNewProducerApplication(ProducerApplication application) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_PRODUCER_APPLICATION");
        notification.put("applicationId", application.getApplicationId());
        notification.put("businessName", application.getBusinessName());
        notification.put("customerEmail", application.getCustomer().getEmail());
        notification.put("timestamp", application.getCreatedAt());

        webSocketHandler.sendToRole(Role.ADMIN.name(), notification);
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

        webSocketService.sendToRole(Role.ADMIN.name(), notification);
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

        webSocketService.sendToRole(Role.ADMIN.name(), notification);
    }
} 