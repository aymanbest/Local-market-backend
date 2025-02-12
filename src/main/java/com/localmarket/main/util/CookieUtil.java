package com.localmarket.main.util;

import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import com.localmarket.main.security.CustomUserDetails;
import com.localmarket.main.service.auth.JwtService;
import java.util.HashMap;
import java.util.Map;


@Component
public class CookieUtil {
    private static final String JWT_COOKIE_NAME = "jwt";
    private static final int COOKIE_EXPIRY = 24 * 60 * 60; // 24 hours in seconds

    private final JwtService jwtService;

    public CookieUtil(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(COOKIE_EXPIRY)
                .build();
    }

    public ResponseCookie deleteJwtCookie() {
        return ResponseCookie.from(JWT_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }

    public String getJwtFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JWT_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public String createJwtFromUserDetails(CustomUserDetails userDetails) {
        if (userDetails == null) return null;
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("username", userDetails.getUsername());
        claims.put("firstname", userDetails.getFirstname());
        claims.put("lastname", userDetails.getLastname());
        claims.put("email", userDetails.getEmail());
        claims.put("role", userDetails.getRole().name());
        claims.put("tokenVersion", userDetails.getTokenVersion());
        claims.put("applicationStatus", userDetails.getApplicationStatus());
        
        return jwtService.generateToken(claims, userDetails.getEmail());
    }
} 