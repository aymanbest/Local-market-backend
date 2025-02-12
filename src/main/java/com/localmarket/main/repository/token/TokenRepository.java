package com.localmarket.main.repository.token;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;


@Service
public class TokenRepository {
    private static class TokenInfo {
        final Long userId;
        final LocalDateTime expiresAt;
        
        TokenInfo(Long userId, LocalDateTime expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }
    
    private final Map<String, TokenInfo> tokenStore = new ConcurrentHashMap<>();
    private final Map<Long, String> userActiveTokens = new ConcurrentHashMap<>();
    
    public void storeToken(String token, Long userId) {
        // Get existing token before storing new one
        userActiveTokens.get(userId);
        
        // Store new token and update active token
        tokenStore.put(token, new TokenInfo(userId, LocalDateTime.now().plusHours(24)));
        userActiveTokens.put(userId, token);
        
        // Don't remove old token from tokenStore - let it be removed when isTokenValid checks it
    }
    
    public void invalidateToken(String token) {
        TokenInfo info = tokenStore.get(token);
        if (info != null) {
            userActiveTokens.remove(info.userId);
            tokenStore.remove(token);
        }
    }
    
    public boolean isTokenValid(String token) {
        TokenInfo info = tokenStore.get(token);
        if (info == null) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Token not found");
        }
        
        // Check if this is still the active token for this user
        String activeToken = userActiveTokens.get(info.userId);
        if (!token.equals(activeToken)) {
            tokenStore.remove(token);  // Only remove old token after we've determined it's not active
            throw new ApiException(ErrorType.INVALID_SESSION, "Session has been invalidated by new login");
        }
        
        if (info.expiresAt.isBefore(LocalDateTime.now())) {
            invalidateToken(token);
            throw new ApiException(ErrorType.TOKEN_EXPIRED, "Token has expired");
        }
        return true;
    }
    
    public void invalidateUserTokens(Long userId) {
        String token = userActiveTokens.remove(userId);
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}