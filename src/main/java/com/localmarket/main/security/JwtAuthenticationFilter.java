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
import com.localmarket.main.service.JwtService;
import com.localmarket.main.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.io.IOException;
import java.util.Collections;
import com.localmarket.main.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import com.localmarket.main.dto.ErrorResponse;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

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

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            handleUnauthorizedResponse(response, request, "Authentication required");
            return;
        }

        String jwt = authHeader.substring(7);
        
        if (!jwtService.isTokenValid(jwt)) {
            handleUnauthorizedResponse(response, request, "Invalid or expired token");
            return;
        }

        String userEmail = jwtService.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()))
            );
            
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
            
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Auth endpoints
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        
        // Public GET endpoints
        if ("GET".equals(method)) {
            return path.startsWith("/api/products") || 
                   path.startsWith("/api/categories");
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