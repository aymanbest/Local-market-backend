package com.localmarket.main.controller.auth;

import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;
import com.localmarket.main.dto.auth.LogoutResponse;
import com.localmarket.main.dto.auth.UserInfoResponse;

import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.dto.auth.AuthServiceResult;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;
import com.localmarket.main.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.jsonwebtoken.Claims;
import com.localmarket.main.service.auth.JwtService;
import com.localmarket.main.repository.token.TokenRepository;
import com.localmarket.main.exception.ApiException;
import com.localmarket.main.exception.ErrorType;



@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")

public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtService jwtService;
    private final TokenRepository tokenRepository;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the provided details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully registered", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String adminJwt = null;
        try {
            adminJwt = cookieUtil.getJwtFromRequest(httpRequest);
        } catch (Exception e) {
            // Ignore if no token present - normal for customer registration
        }
        
        AuthServiceResult result = authService.register(request, adminJwt);
        response.addHeader(HttpHeaders.SET_COOKIE, 
            cookieUtil.createJwtCookie(result.getToken()).toString());
            
        return ResponseEntity.ok(result.getResponse());
    }

    @Operation(
        summary = "Login user",
        description = "Authenticates user and returns JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request,
            HttpServletResponse response) {
                AuthServiceResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, 
            cookieUtil.createJwtCookie(result.getToken()).toString());
        return ResponseEntity.ok(result.getResponse());
    }

    @Operation(
        summary = "Logout user",
        description = "Invalidates the JWT token"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out", content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        authService.logout(jwt);
        response.addHeader(HttpHeaders.SET_COOKIE, 
            cookieUtil.deleteJwtCookie().toString());
        return ResponseEntity.ok(LogoutResponse.builder()
                .status("Logout successful")
                .build());
    }

    @Operation(
        summary = "Get current user info",
        description = "Retrieves the current authenticated user's information from JWT"
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User info retrieved successfully", 
            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(HttpServletRequest request) {
        String jwt = cookieUtil.getJwtFromRequest(request);
        tokenRepository.isTokenValid(jwt);
        Claims claims = jwtService.extractAllClaims(jwt);
        
        return ResponseEntity.ok(UserInfoResponse.builder()
                .userId(claims.get("userId", Long.class))
                .username(claims.get("username", String.class))
                .firstname(claims.get("firstname", String.class))
                .lastname(claims.get("lastname", String.class))
                .email(claims.get("email", String.class))
                .role(claims.get("role", String.class))
                .applicationStatus(claims.get("applicationStatus", String.class))
                .build());
    }
} 
