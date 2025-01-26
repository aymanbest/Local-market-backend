package com.localmarket.main.controller.auth;

import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;
import com.localmarket.main.dto.auth.LogoutResponse;

import com.localmarket.main.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.localmarket.main.dto.error.ErrorResponse;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    private final AuthService authService;

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
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.register(request, authHeader));
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
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
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
    public ResponseEntity<LogoutResponse> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok(LogoutResponse.builder()
                .status("Logout successful")
                .build());
    }
} 