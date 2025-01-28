package com.localmarket.main.service.notification.producer;

import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.product.Product;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.service.notification.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProducerNotificationService {
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    public void sendToUser(Long producerId, NotificationResponse notification) {
        User producer = userRepository.findById((producerId))
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "Producer not found"));
            
        webSocketService.sendToUser(producer.getEmail(), notification);
    }

    public void notifyNewOrder(Long producerId, Order order) {
        User producer = userRepository.findById(producerId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "Producer not found"));

        NotificationResponse notification = NotificationResponse.builder()
            .type("NEW_ORDER")
            .message("New order received")
            .data(order)
            .timestamp(LocalDateTime.now())
            .build();

        
        webSocketService.sendToUser(producer.getEmail(), notification);
    }

    public void notifyProductApproval(Long producerId, Product product, boolean approved, String reason) {
        User producer = userRepository.findById(producerId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "Producer not found"));

        NotificationResponse notification = NotificationResponse.builder()
            .type(approved ? "PRODUCT_APPROVED" : "PRODUCT_REJECTED")
            .message(approved ? "Product approved" : "Product rejected: " + reason)
            .data(product)
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToUser(producer.getEmail(), notification);
    }

    public void notifyLowStock(Long producerId, Product product) {
        User producer = userRepository.findById(producerId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "Producer not found"));

        NotificationResponse notification = NotificationResponse.builder()
            .type("LOW_STOCK")
            .message("Low stock alert for " + product.getName())
            .data(product)
            .timestamp(LocalDateTime.now())
            .build();

        webSocketService.sendToUser(producer.getEmail(), notification);
    }

} 