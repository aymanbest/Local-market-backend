package com.localmarket.main.util;

import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;


@Component
public class CookieUtil {
    private static final String JWT_COOKIE_NAME = "jwt";
    private static final int COOKIE_EXPIRY = 24 * 60 * 60; // 24 hours in seconds

    public ResponseCookie createJwtCookie(String token) {
        return ResponseCookie.from(JWT_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)  // Enable for HTTPS
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

    public String getJwtFromRequest(HttpServletRequest request) {
        String jwt = getJwtFromCookies(request);
        if (jwt == null) {
            throw new ApiException(ErrorType.INVALID_SESSION, "Missing authentication token");
        }
        return jwt;
    }

    public String extractJwtFromCookie(String cookieHeader) {
        if (cookieHeader == null) return null;
        
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            if (cookie.trim().startsWith("jwt=")) {
                return cookie.trim().substring(4);
            }
        }
        return null;
    }
} 