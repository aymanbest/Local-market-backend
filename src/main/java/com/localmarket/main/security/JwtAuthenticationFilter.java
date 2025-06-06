package com.localmarket.main.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.repository.user.UserRepository;
import com.localmarket.main.entity.user.User;
import com.localmarket.main.repository.token.TokenRepository;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;
import com.localmarket.main.util.CookieUtil;
import java.io.IOException;
import java.util.Collections;
import com.localmarket.main.entity.user.Role;
import com.localmarket.main.repository.producer.ProducerApplicationRepository;
import org.springframework.security.core.Authentication;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final CookieUtil cookieUtil;
    private final ProducerApplicationRepository applicationRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            if (isAuthEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = cookieUtil.getJwtFromCookies(request);
            
            if (jwt != null) {
                // Always validate token and authenticate
                if (!jwtService.isTokenValid(jwt) || !tokenRepository.isTokenValid(jwt)) {
                    SecurityContextHolder.clearContext();
                    throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid token");
                }
                authenticateUser(jwt);
            }

            if (isPublicEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                throw new ApiException(ErrorType.INVALID_TOKEN, "Missing authentication token");
            }

            filterChain.doFilter(request, response);
            
        } catch (ApiException e) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(e.getMessage());
        }
    }

    private void authenticateUser(String jwt) {
        // Verify token is still valid in TokenRepository
        if (!tokenRepository.isTokenValid(jwt)) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Token has been invalidated");
        }

        String userEmail = jwtService.extractUsername(jwt);
        String role = jwtService.extractRole(jwt);
        Long userId = jwtService.extractUserId(jwt);
        Integer tokenVersion = jwtService.extractTokenVersion(jwt);

        // Double check token version matches current user version
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(ErrorType.USER_NOT_FOUND, "User not found"));

        if (!tokenVersion.equals(user.getTokenVersion())) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Token has been invalidated");
        }

        String applicationStatus = user.getRole() == Role.CUSTOMER ?
            applicationRepository.findByCustomer(user)
                .map(app -> app.getStatus().name())
                .orElse("NO_APPLICATION") :
            null;

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails = CustomUserDetails.builder()
                .id(userId)
                .email(userEmail)
                .username(user.getUsername())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .role(user.getRole())
                .tokenVersion(tokenVersion)
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .applicationStatus(applicationStatus)
                .build();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String accessToken = request.getParameter("accessToken");

        // Swagger UI endpoints
        if (path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/webjars/")) {
            return true;
        }

        // WebSocket handshake endpoint
        if (path.equals("/ws") && "GET".equals(method)) {
            return true;
        }

        // Auth endpoints except /me and /logout
        if (path.startsWith("/api/auth/")) {
            return !path.equals("/api/auth/me") && !path.equals("/api/auth/logout");
        }

        // Public GET endpoints
        if ("GET".equals(method)) {
            return path.startsWith("/api/regions") ||
                   path.startsWith("/api/categories") ||
                   (path.startsWith("/api/products") && !path.contains("/my-products") && 
                    !path.contains("/my-pending") && !path.contains("/pending")) ||
                   (path.startsWith("/api/reviews/product/") && !path.contains("/pending") && 
                    !path.contains("/eligibility")) ||
                   ((path.startsWith("/api/orders/") || path.equals("/api/orders")) && 
                    accessToken != null) ||
                   (path.startsWith("/api/coupons/validate/"));
        }

        // Allow guest checkout
        if ("POST".equals(method)) {
            return path.equals("/api/orders/checkout") ||
                   (path.equals("/api/orders/pay") && accessToken != null) ||
                   path.startsWith("/api/orders/bundle/");
        }

        return false;
    }

    private boolean isAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") && 
               !path.equals("/api/auth/me") && 
               !path.equals("/api/auth/logout");
    }
}