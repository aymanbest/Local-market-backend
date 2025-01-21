package com.localmarket.main.security;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.localmarket.main.service.auth.JwtService;

import io.jsonwebtoken.Claims;

@Service
public class TokenBlacklist {
    private final Set<String> blacklistedTokens = Collections.synchronizedSet(new HashSet<>());
    private final Map<Long, LocalDateTime> userInvalidationTimes = new ConcurrentHashMap<>();

    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public void blacklistUserTokens(Long userId) {
        userInvalidationTimes.put(userId, LocalDateTime.now());
    }

    public boolean isTokenInvalidated(Long userId, LocalDateTime tokenIssuedAt) {
        LocalDateTime invalidationTime = userInvalidationTimes.get(userId);
        return invalidationTime != null && tokenIssuedAt.isBefore(invalidationTime);
    }
} 