package com.localmarket.main.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import com.localmarket.main.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> roleSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<WebSocketSession, String> sessionTokens = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void startTokenValidator() {
        scheduler.scheduleAtFixedRate(this::validateActiveSessions, 0, 30, TimeUnit.SECONDS);
    }
    
    private void validateActiveSessions() {
        userSessions.values().forEach(session -> {
            String token = sessionTokens.get(session);
            if (token != null && !jwtService.isTokenValid(token)) {
                closeSession(session, "Token invalidated or expired");
            }
        });
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String token = extractToken(session);
            sessionTokens.put(session, token);
            Map<String, String> authInfo = extractAuthInfo(session);
            
            userSessions.put(authInfo.get("email"), session);
            roleSessions.computeIfAbsent(authInfo.get("role"), k -> new ConcurrentSkipListSet<>())
                .add(authInfo.get("email"));
            
            log.info("WebSocket connection established for user: {} with role: {}", authInfo.get("email"), authInfo.get("role"));
        } catch (Exception e) {
            log.error("Failed to establish WebSocket connection: {}", e.getMessage());
            closeSession(session, "Invalid or expired token");
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
        sessionTokens.remove(session);
        Map<String, String> authInfo = extractAuthInfo(session);
        String email = authInfo.get("email");
        String role = authInfo.get("role");
        
        userSessions.remove(email);
        if (roleSessions.containsKey(role)) {
            roleSessions.get(role).remove(email);
        }
    }
    
    public void sendNotification(String email, Object notification) {
        WebSocketSession session = userSessions.get(email);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(message));
              
            } catch (IOException e) {
                log.error("Error sending notification to user: {}", email, e);
            }
        }
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
    
    private Map<String, String> extractAuthInfo(WebSocketSession session) {
        String token = session.getHandshakeHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            
            // Validate token
            if (!jwtService.isTokenValid(token)) {
                throw new IllegalStateException("Token is expired or invalid");
            }
            
            return Map.of(
                "email", jwtService.extractUsername(token),
                "role", jwtService.extractRole(token)
            );
        }
        throw new IllegalStateException("Invalid authorization token");
    }
    
    private String extractToken(WebSocketSession session) {
        String auth = session.getHandshakeHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        throw new IllegalStateException("Invalid authorization token");
    }
} 