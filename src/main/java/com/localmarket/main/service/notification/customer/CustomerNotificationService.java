package com.localmarket.main.service.notification.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.service.notification.WebSocketService;
import com.localmarket.main.dto.notification.NotificationResponse;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerNotificationService {
    private final WebSocketService webSocketService;

    public void notifyOrderStatusUpdate(Order order) {
        NotificationResponse notification = NotificationResponse.builder()
            .type("ORDER_STATUS_UPDATE")
            .message(String.format("Order #%d status updated to: %s", 
                order.getOrderId(), order.getStatus()))
            .data(order)
            .timestamp(LocalDateTime.now())
            .read(false)
            .build();

        if (order.getCustomer() != null) {
            webSocketService.sendToUser(order.getCustomer().getEmail(), notification);
        } else if (order.getGuestEmail() != null) {
            webSocketService.sendToUser(order.getGuestEmail(), notification);
        }
    }

    public void notifyDeliveryUpdate(Order order, String updateMessage) {
        NotificationResponse notification = NotificationResponse.builder()
            .type("DELIVERY_UPDATE")
            .message(updateMessage)
            .data(order)
            .timestamp(LocalDateTime.now())
            .read(false)
            .build();

        if (order.getCustomer() != null) {
            webSocketService.sendToUser(order.getCustomer().getEmail(), notification);
        } else if (order.getGuestEmail() != null) {
            webSocketService.sendToUser(order.getGuestEmail(), notification);
        }
    }
} 