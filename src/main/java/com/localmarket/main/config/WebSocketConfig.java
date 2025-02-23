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
import com.localmarket.main.util.CookieUtil;
import com.localmarket.main.repository.token.TokenRepository;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final NotificationWebSocketHandler webSocketHandler;
    private final CookieUtil cookieUtil;
    private final TokenRepository tokenRepository;
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
            .setAllowedOrigins(frontendUrl)
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
        return new CustomHandshakeInterceptor(cookieUtil, tokenRepository);
    }
} 