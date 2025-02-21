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
            storedNotification.setData(objectMapper.writeValueAsString(notification.getData()));
            storedNotification.setTimestamp(notification.getTimestamp());
            storedNotification.setExpiresAt(LocalDateTime.now().plusDays(7));
            
            return storedNotificationRepository.save(storedNotification);
        } catch (Exception e) {
            log.error("Failed to store notification for user {}: {}", email, e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Failed to store notification");
        }
    }

    public void sendToRole(String role, NotificationResponse notification) {
        webSocketHandler.sendToRole(role, notification);
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