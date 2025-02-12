package com.localmarket.main.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.util.CookieUtil;
import com.localmarket.main.entity.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.localmarket.main.security.CustomUserDetails;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.localmarket.main.repository.token.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;
import com.localmarket.main.entity.user.Role;

@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger log = LoggerFactory.getLogger(CustomHandshakeInterceptor.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;
    private final TokenRepository tokenRepository;
    private final ProducerApplicationRepository applicationRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            
            try {
                String jwt = cookieUtil.getJwtFromCookies(httpServletRequest);
                log.debug("JWT from cookies: {}", jwt != null ? "present" : "not present");
                
                if (jwt == null || !tokenRepository.isTokenValid(jwt)) {
                    log.warn("Invalid or missing JWT token");
                    return false;
                }

                // Extract user details from JWT
                Long userId = jwtService.extractUserId(jwt);
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

                if (!jwtService.extractTokenVersion(jwt).equals(user.getTokenVersion())) {
                    log.warn("Token version mismatch");
                    return false;
                }

                String applicationStatus = user.getRole() == Role.CUSTOMER ?
                    applicationRepository.findByCustomer(user)
                        .map(app -> app.getStatus().name())
                        .orElse("NO_APPLICATION") : 
                    null;

                // Create CustomUserDetails
                CustomUserDetails userDetails = CustomUserDetails.builder()
                    .id(userId)
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .role(user.getRole())
                    .tokenVersion(user.getTokenVersion())
                    .password("")
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
                    .applicationStatus(applicationStatus)
                    .build();

                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                attributes.put("user", userDetails);
                attributes.put("authentication", authentication);
                
                log.info("WebSocket handshake successful for user: {}", userDetails.getEmail());
                return true;
            } catch (Exception e) {
                log.error("Error during WebSocket handshake: {}", e.getMessage());
                return false;
            }
        }
        log.warn("Request is not a ServletServerHttpRequest");
        return false;
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