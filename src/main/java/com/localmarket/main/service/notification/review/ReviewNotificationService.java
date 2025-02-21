package com.localmarket.main.service.notification.review;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.localmarket.main.service.notification.WebSocketService;
import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.entity.review.Review;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewNotificationService {
    private final WebSocketService webSocketService;

    public void notifyReviewStatusUpdate(Review review) {
        NotificationResponse notification = NotificationResponse.builder()
            .type("REVIEW_STATUS_UPDATE")
            .message(String.format("Your review for product '%s' has been %s", 
                review.getProduct().getName(), 
                review.getStatus().toString().toLowerCase()))
            .data(review)
            .timestamp(LocalDateTime.now())
            .read(false)
            .build();

        webSocketService.sendToUser(review.getCustomer().getEmail(), notification);
    }
} 