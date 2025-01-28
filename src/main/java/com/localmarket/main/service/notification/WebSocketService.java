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

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    private final NotificationWebSocketHandler webSocketHandler;
    private final UserRepository userRepository;

    public void sendToUser(String identifier, NotificationResponse notification) {
       
        
        if (identifier.matches("\\d+")) {
            User user = userRepository.findById(Long.parseLong(identifier))
                .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));
            webSocketHandler.sendNotification(user.getEmail(), notification);
        } else {
            webSocketHandler.sendNotification(identifier, notification);
        }
        
    
    }

    public void sendToRole(String role, NotificationResponse notification) {
        webSocketHandler.sendToRole(role, notification);
    }
} 