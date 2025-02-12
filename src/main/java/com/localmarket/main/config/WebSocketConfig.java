package com.localmarket.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import lombok.RequiredArgsConstructor;
import com.localmarket.main.websocket.NotificationWebSocketHandler;
import com.localmarket.main.websocket.CustomHandshakeInterceptor;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.util.CookieUtil;
import com.localmarket.main.repository.token.TokenRepository;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler webSocketHandler;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;
    private final TokenRepository tokenRepository;
    private final ProducerApplicationRepository applicationRepository;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
            .setAllowedOrigins("http://localhost:5173")
            .addInterceptors(securityInterceptor());
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        container.setMaxSessionIdleTimeout(600000L);
        return container;
    }

    @Bean
    public CustomHandshakeInterceptor securityInterceptor() {
        return new CustomHandshakeInterceptor(jwtService, userRepository, cookieUtil, tokenRepository, applicationRepository);
    }
} 