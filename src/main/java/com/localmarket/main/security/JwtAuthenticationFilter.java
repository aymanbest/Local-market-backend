package com.localmarket.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.repository.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.io.IOException;
import java.util.Collections;

import com.localmarket.main.dto.error.ErrorResponse;
import com.localmarket.main.entity.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;

import com.localmarket.main.repository.token.TokenRepository;
import com.localmarket.main.exception.ApiException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final TokenRepository tokenRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip authentication for public endpoints
        if (isPublicEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = request.getParameter("accessToken");
        final String authHeader = request.getHeader("Authorization");

        // Allow requests with accessToken without requiring Authorization header
        if (accessToken != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // For all other protected endpoints, require Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            handleUnauthorizedResponse(response, request, "Missing or invalid authorization header");
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // Verify token is still valid in TokenRepository
            if (!tokenRepository.isTokenValid(jwt)) {
                handleUnauthorizedResponse(response, request, "Token has been invalidated");
                return;
            }

            String userEmail = jwtService.extractUsername(jwt);
            String role = jwtService.extractRole(jwt);
            Long userId = jwtService.extractUserId(jwt);
            Integer tokenVersion = jwtService.extractTokenVersion(jwt);

            // Double check token version matches current user version
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!tokenVersion.equals(user.getTokenVersion())) {
                handleUnauthorizedResponse(response, request, "Token has been invalidated");
                return;
            }

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        userEmail,
                        "", // No need for password as we're using token-based auth
                        Collections.singletonList(new SimpleGrantedAuthority(role)));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);
        } catch (ApiException e) {
            handleUnauthorizedResponse(response, request, e.getMessage());
            return;
        }
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String accessToken = request.getParameter("accessToken");

        // Add specific check for orders endpoint with accessToken
        if ("GET".equals(method) && path.equals("/api/orders") && accessToken != null) {
            return true;
        }

        // Swagger UI endpoints
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/webjars/")) {
            return true;
        }

        // WebSocket endpoints
        if (path.startsWith("/ws") || path.startsWith("/websocket/")) {
            return true;
        }

        // Auth endpoints
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Public GET endpoints for products
        if ("GET".equals(method) && path.startsWith("/api/products")) {
            // Exclude protected product endpoints
            return !path.contains("/my-products") && 
                   !path.contains("/my-pending") && 
                   !path.contains("/pending");
        }

        // Other public GET endpoints
        if ("GET".equals(method)) {
            return path.startsWith("/api/categories") ||
                    (path.startsWith("/api/orders/") && accessToken != null);
        }

        // Allow guest orders
        if ("POST".equals(method)) {
            return path.equals("/api/orders/checkout") ||
                    path.matches("/api/orders/\\d+/pay");
        }

        // Public GET endpoints for reviews
        if ("GET".equals(method) && path.startsWith("/api/reviews")) {
            return path.startsWith("/api/reviews/product/") && 
                   !path.contains("/pending") &&
                   !path.contains("/eligibility");
        }

        return false;
    }

    private boolean isPublicPath(String path, String method) {
        // Swagger UI endpoints
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html")) {
            return true;
        }

        // Auth endpoints
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        // Public GET endpoints for products
        if ("GET".equals(method) && path.startsWith("/api/products")) {
            // Exclude protected product endpoints
            return !path.contains("/my-products") && 
                   !path.contains("/my-pending") && 
                   !path.contains("/pending");
        }

        // Other public GET endpoints
        if ("GET".equals(method)) {
            return path.startsWith("/api/categories") ||
                    (path.startsWith("/api/orders/") && path.contains("?accessToken="));
        }

        // Allow guest orders
        if ("POST".equals(method)) {
            return path.equals("/api/orders/checkout") ||
                    path.matches("/api/orders/\\d+/pay");
        }

        // New condition for producer orders
        if ("GET".equals(method) && path.startsWith("/api/orders/")) {
            return !path.contains("/producer-orders") && 
                   (path.contains("?accessToken=") || path.contains("/status/"));
        }

        // Add in isPublicPath method after Swagger endpoints check
        if (path.startsWith("/ws")) {
            return true;
        }

        // WebSocket endpoints
        if (path.startsWith("/ws") || path.startsWith("/websocket/")) {
            return true;
        }

        return false;
    }

    private void handleUnauthorizedResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}