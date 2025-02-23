package com.localmarket.main.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;
import com.localmarket.main.util.CookieUtil;
import com.localmarket.main.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.localmarket.main.repository.token.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(CustomHandshakeInterceptor.class);
    private final CookieUtil cookieUtil;
    private final TokenRepository tokenRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("Request is not a ServletServerHttpRequest");
            return false;
        }

        try {
            String jwt = cookieUtil.getJwtFromCookies(servletRequest.getServletRequest());
            if (jwt == null || !tokenRepository.isTokenValid(jwt)) {
                log.warn("Invalid or missing JWT token");
                return false;
            }

            // Get existing authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
                log.warn("No valid authentication found in SecurityContext");
                return false;
            }

            // Reuse existing CustomUserDetails
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            attributes.put("user", userDetails);
            attributes.put("authentication", authentication);
            
            log.info("WebSocket handshake successful for user: {}", userDetails.getEmail());
            return true;
        } catch (Exception e) {
            log.error("Error during WebSocket handshake: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Clean up if needed
        if (exception != null) {
            log.error("Error after handshake: {}", exception.getMessage());
        }
    }
} 