package com.localmarket.main.service.auth;

import org.springframework.stereotype.Service;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import com.localmarket.main.entity.AccessToken.AccessToken;
import com.localmarket.main.repository.accesstoken.AccessTokenRepository;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;

// TokenService is a service that handles access tokens for users. 
// It generates and validates access tokens, and schedules cleanup of expired tokens.
// TODO: In Future it gonna be used in emails to users to check there orders
// TODO: Can be used in password reset as well
@Service
@RequiredArgsConstructor
public class TokenService {
    
    private final AccessTokenRepository accessTokenRepository;

    public String getOrCreateAccessToken(String email) {
        return accessTokenRepository.findByEmail(email)
            .map(AccessToken::getToken)
            .orElseGet(() -> {
                AccessToken token = new AccessToken();
                token.setToken(UUID.randomUUID().toString());
                token.setEmail(email);
                token.setExpiresAt(LocalDateTime.now().plusDays(7));
                token.setCreatedAt(LocalDateTime.now());
                accessTokenRepository.save(token);
                return token.getToken();
            });
    }

    public String validateAccessToken(String token) {
        return accessTokenRepository
            .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
            .map(AccessToken::getEmail)
            .orElse(null);
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    public void cleanupExpiredTokens() {
        accessTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
} 