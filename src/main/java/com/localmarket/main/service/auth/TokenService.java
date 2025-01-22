package com.localmarket.main.service.auth;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class TokenService {
    
    public String generateGuestToken(String guestEmail) {
        // Generate a unique token using UUID
        return UUID.randomUUID().toString();
    }

    public boolean validateGuestToken(String token, LocalDateTime expiresAt) {
        if (token == null || expiresAt == null) {
            return false;
        }
        
        // Check if the token has expired
        return LocalDateTime.now().isBefore(expiresAt);
    }
} 