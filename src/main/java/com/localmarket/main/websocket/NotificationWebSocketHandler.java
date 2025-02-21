package com.localmarket.main.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import com.localmarket.main.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.List;
import java.time.LocalDateTime;
import com.localmarket.main.entity.notification.StoredNotification;
import com.localmarket.main.repository.notification.StoredNotificationRepository;
import com.localmarket.main.dto.notification.NotificationResponse;
import com.localmarket.main.service.notification.NotificationStorageService;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final StoredNotificationRepository storedNotificationRepository;
    private final NotificationStorageService notificationStorageService;

    private static final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> roleSessions = new ConcurrentHashMap<>();
    private static final String HEARTBEAT_MESSAGE = "{\"type\":\"heartbeat\"}";

    @PostConstruct
    public void startTokenValidator() {
        scheduler.scheduleAtFixedRate(this::validateActiveSessions, 0, 30, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 0, 240, TimeUnit.SECONDS); // 4 minutes
    }

    private void sendHeartbeat() {
        userSessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(HEARTBEAT_MESSAGE));
                } catch (IOException e) {
                    log.warn("Failed to send heartbeat to session: {}", e.getMessage());
                    closeSession(session, "Failed to send heartbeat");
                }
            }
        });
    }

    private void validateActiveSessions() {
        userSessions.values().forEach(session -> {
            if (session.getPrincipal() == null || !session.isOpen()) {
                closeSession(session, "Invalid session");
            }
        });
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            CustomUserDetails userDetails = extractUserDetails(session);
            if (userDetails == null) {
                closeSession(session, "Authentication required");
                return;
            }

            userSessions.put(userDetails.getEmail(), session);
            roleSessions.computeIfAbsent(userDetails.getRole().name(), k -> new ConcurrentSkipListSet<>())
                    .add(userDetails.getEmail());

            // Send stored notifications
            sendStoredNotifications(userDetails.getEmail(), session);

            log.info("WebSocket connection established for user: {} with role: {}", 
                userDetails.getEmail(), userDetails.getRole());
        } catch (Exception e) {
            log.error("Failed to establish WebSocket connection: {}", e.getMessage());
            closeSession(session, "Authentication failed");
        }
    }

    private void sendStoredNotifications(String email, WebSocketSession session) {
        List<StoredNotification> notifications = storedNotificationRepository
            .findByRecipientEmailAndReadFalseAndExpiresAtGreaterThan(email, LocalDateTime.now());
            
        for (StoredNotification notification : notifications) {
            try {
                NotificationResponse response = NotificationResponse.builder()
                    .id(notification.getId())
                    .type(notification.getType())
                    .message(notification.getMessage())
                    .data(objectMapper.readValue(notification.getData(), Object.class))
                    .timestamp(notification.getTimestamp())
                    .read(notification.isRead())
                    .build();
                    
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            } catch (Exception e) {
                log.error("Failed to send stored notification: {}", e.getMessage());
            }
        }
    }

    private void closeSession(WebSocketSession session, String reason) {
        try {
            session.close(new CloseStatus(4001, reason));
        } catch (IOException e) {
            log.error("Error closing WebSocket session: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            CustomUserDetails userDetails = extractUserDetails(session);
            if (userDetails != null) {
                String email = userDetails.getEmail();
                String role = userDetails.getRole().name();

                userSessions.remove(email);
                if (roleSessions.containsKey(role)) {
                    roleSessions.get(role).remove(email);
                }
                
                log.info("WebSocket connection closed for user: {} with role: {} - Status: {}", 
                    email, role, status.getReason() != null ? status.getReason() : "Connection closed normally");
            }
        } catch (Exception e) {
            log.info("WebSocket connection closed - Status: {}", 
                status.getReason() != null ? status.getReason() : "Connection closed normally");
        }
    }

    private CustomUserDetails extractUserDetails(WebSocketSession session) {
        if (session.getPrincipal() instanceof UsernamePasswordAuthenticationToken auth &&
            auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }

    public boolean sendNotification(String email, Object notification) {
        WebSocketSession session = userSessions.get(email);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(message));
                return true;
            } catch (IOException e) {
                log.error("Error sending notification to user: {}", email, e);
            }
        }
        return false;
    }

    public void sendToRole(String role, Object notification) {
        Set<String> users = roleSessions.get(role.toUpperCase());
        if (users != null) {
            String message;
            try {
                message = objectMapper.writeValueAsString(notification);
                for (String email : users) {
                    WebSocketSession session = userSessions.get(email);
                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            } catch (IOException e) {
                log.error("Error sending notification to role: {}", role, e);
            }
        }
    }

    public void closeUserSessions(String userEmail) {
        WebSocketSession session = userSessions.get(userEmail);
        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseStatus(4000, "User logged out"));
                userSessions.remove(userEmail);
                
                // Remove from role sessions
                roleSessions.values().forEach(users -> users.remove(userEmail));
                
                log.info("Closed WebSocket session for user {} due to logout", userEmail);
            } catch (IOException e) {
                log.error("Error closing WebSocket session for user {}: {}", userEmail, e.getMessage());
            }
        }
    }
}