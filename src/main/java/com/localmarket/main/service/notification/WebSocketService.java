package com.localmarket.main.service.notification;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.websocket.NotificationWebSocketHandler;
import com.localmarket.main.entity.notification.StoredNotification;
import com.localmarket.main.repository.notification.StoredNotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import com.localmarket.main.entity.user.Role;
import java.util.Map;
import java.util.HashMap;
import com.localmarket.main.entity.order.Order;
import com.localmarket.main.entity.product.Product;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    private final NotificationWebSocketHandler webSocketHandler;
    private final UserRepository userRepository;
    private final StoredNotificationRepository storedNotificationRepository;
    private final ObjectMapper objectMapper;

    public void sendToUser(String identifier, NotificationResponse notification) {
        String email;
        
        if (identifier.matches("\\d+")) {
            User user = userRepository.findById(Long.parseLong(identifier))
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));
            email = user.getEmail();
        } else {
            email = identifier;
        }

        // Store notification first
        StoredNotification storedNotification = storeNotification(email, notification);
        
        // Update notification with stored ID and send
        NotificationResponse notificationWithId = NotificationResponse.builder()
            .id(storedNotification.getId())
            .type(notification.getType())
            .message(notification.getMessage())
            .data(notification.getData())
            .timestamp(notification.getTimestamp())
            .read(false)
            .build();
        
        // Then try to send via WebSocket if user is online
        try {
            webSocketHandler.sendNotification(email, notificationWithId);
        } catch (Exception e) {
            log.debug("User {} is offline, notification stored for later delivery", email);
        }
    }

    private StoredNotification storeNotification(String email, NotificationResponse notification) {
        try {
            StoredNotification storedNotification = new StoredNotification();
            storedNotification.setRecipientEmail(email);
            storedNotification.setType(notification.getType());
            storedNotification.setMessage(notification.getMessage());
            
            // Use the injected ObjectMapper instead of creating a new one
            // Convert complex objects to simple maps if needed
            Object data = notification.getData();
            if (data instanceof Order || data instanceof Product) {
                Map<String, Object> simpleData = new HashMap<>();
                if (data instanceof Order) {
                    Order order = (Order) data;
                    simpleData.put("orderId", order.getOrderId());
                    simpleData.put("status", order.getStatus());
                    simpleData.put("totalPrice", order.getTotalPrice());
                } else if (data instanceof Product) {
                    Product product = (Product) data;
                    simpleData.put("productId", product.getProductId());
                    simpleData.put("name", product.getName());
                    simpleData.put("price", product.getPrice());
                }
                storedNotification.setData(objectMapper.writeValueAsString(simpleData));
            } else {
                storedNotification.setData(objectMapper.writeValueAsString(data));
            }
            
            storedNotification.setTimestamp(notification.getTimestamp());
            storedNotification.setExpiresAt(LocalDateTime.now().plusDays(7));
            
            return storedNotificationRepository.save(storedNotification);
        } catch (Exception e) {
            log.error("Failed to store notification for user {}: {}", email, e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Failed to store notification");
        }
    }

    public void sendToRole(String role, NotificationResponse notification) {
        // Get all users with this role
        List<User> usersWithRole = userRepository.findByRole(Role.valueOf(role.toUpperCase()));
        
        if (usersWithRole.isEmpty()) {
            log.debug("No users found with role: {}", role);
            return;
        }
        
        // Batch store notifications for all users with this role
        List<StoredNotification> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = now.plusDays(7);
        String dataJson;
        
        try {
            dataJson = objectMapper.writeValueAsString(notification.getData());
        } catch (Exception e) {
            log.error("Failed to serialize notification data: {}", e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Failed to process notification data");
        }
        
        // Create notification objects for all users
        for (User user : usersWithRole) {
            StoredNotification storedNotification = new StoredNotification();
            storedNotification.setRecipientEmail(user.getEmail());
            storedNotification.setType(notification.getType());
            storedNotification.setMessage(notification.getMessage());
            storedNotification.setData(dataJson);
            storedNotification.setTimestamp(notification.getTimestamp() != null ? notification.getTimestamp() : now);
            storedNotification.setExpiresAt(expiryDate);
            notifications.add(storedNotification);
        }
        
        // Batch save all notifications
        try {
            List<StoredNotification> savedNotifications = storedNotificationRepository.saveAll(notifications);
            
            // Send notifications to online users
            for (StoredNotification saved : savedNotifications) {
                NotificationResponse notificationWithId = NotificationResponse.builder()
                    .id(saved.getId())
                    .type(saved.getType())
                    .message(saved.getMessage())
                    .data(notification.getData()) // Use the original data object to avoid re-parsing
                    .timestamp(saved.getTimestamp())
                    .read(false)
                    .build();
                    
                try {
                    webSocketHandler.sendNotification(saved.getRecipientEmail(), notificationWithId);
                } catch (Exception e) {
                    // User is offline, notification is already stored
                    log.debug("User {} is offline, notification stored for later delivery", saved.getRecipientEmail());
                }
            }
        } catch (Exception e) {
            log.error("Failed to store notifications for role {}: {}", role, e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Failed to store notifications");
        }
        
        log.info("Sent notification to {} users with role {}", usersWithRole.size(), role);
    }

    public Page<NotificationResponse> getStoredNotifications(String email, Pageable pageable) {
        return storedNotificationRepository.findByRecipientEmailOrderByTimestampDesc(email, pageable)
            .map(this::convertToNotificationResponse);
    }

    public Long getUnreadCount(String email) {
        return storedNotificationRepository.countByRecipientEmailAndReadFalse(email);
    }

    @Transactional
    public void markAllAsRead(String email) {
        storedNotificationRepository.markAllAsRead(email);
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        StoredNotification notification = storedNotificationRepository.findById(notificationId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Notification not found"));
            
        if (!notification.getRecipientEmail().equals(email)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Access denied");
        }
        
        notification.setRead(true);
        storedNotificationRepository.save(notification);
    }

    @Transactional
    public void deleteNotification(String email, Long notificationId) {
        StoredNotification notification = storedNotificationRepository.findById(notificationId)
            .orElseThrow(() -> new ApiException(ErrorType.RESOURCE_NOT_FOUND, "Notification not found"));
            
        if (!notification.getRecipientEmail().equals(email)) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Access denied");
        }
        
        storedNotificationRepository.delete(notification);
    }

    private NotificationResponse convertToNotificationResponse(StoredNotification notification) {
        Object data;
        try {
            data = objectMapper.readValue(notification.getData(), Object.class);
        } catch (Exception e) {
            log.error("Error deserializing notification data: {}", e.getMessage());
            data = null;
        }

        return NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType())
            .message(notification.getMessage())
            .data(data)
            .timestamp(notification.getTimestamp())
            .read(notification.isRead())
            .build();
    }
} 