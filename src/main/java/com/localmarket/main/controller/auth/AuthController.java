package com.localmarket.main.controller.auth;

import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;
import com.localmarket.main.dto.auth.LogoutResponse;
import com.localmarket.main.dto.auth.UserInfoResponse;
import com.localmarket.main.security.CustomUserDetails;

import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.dto.auth.AuthServiceResult;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.localmarket.main.dto.auth.PasswordResetVerifyRequest;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    private final AuthService authService;
    private final CookieUtil cookieUtil;

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
            @AuthenticationPrincipal CustomUserDetails admin,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String adminJwt = cookieUtil.getJwtFromCookies(httpRequest);
        AuthServiceResult result = authService.register(request, adminJwt);
        
        // Only set cookie if it's a self-registration (not admin creating)
        if (adminJwt == null || adminJwt.isEmpty()) {
            response.addHeader(HttpHeaders.SET_COOKIE, 
                cookieUtil.createJwtCookie(result.getToken()).toString());
        }
        
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
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out", content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response) {
        // Get the actual JWT that was used for authentication
        String jwt = cookieUtil.getJwtFromCookies(request);
        
        // Logout from both HTTP and WebSocket sessions
        authService.logout(jwt, userDetails.getEmail());
        
        response.addHeader(HttpHeaders.SET_COOKIE, 
            cookieUtil.deleteJwtCookie().toString());
        return ResponseEntity.ok(LogoutResponse.builder()
                .status("Logout successful")
                .build());
    }

    @Operation(
        summary = "Get current user info",
        description = "Retrieves the current authenticated user's information"
    )
    @SecurityRequirement(name = "cookie")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User info retrieved successfully", 
            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated", 
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(UserInfoResponse.builder()
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .firstname(userDetails.getFirstname())
                .lastname(userDetails.getLastname())
                .email(userDetails.getEmail())
                .role(userDetails.getRole().name())
                .applicationStatus(userDetails.getApplicationStatus())
                .build());
    }

    @PostMapping("/password-reset-request")
    public ResponseEntity<AuthResponse> requestPasswordReset(
            @RequestBody PasswordResetVerifyRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
            .status(200)
            .message("Password reset code sent to your email")
            .build());
    }

    @PostMapping("/password-reset-verify")
    public ResponseEntity<AuthResponse> verifyAndResetPassword(
            @RequestBody PasswordResetVerifyRequest request) {
        authService.verifyAndResetPassword(
            request.getEmail(), 
            request.getCode(), 
            request.getNewPassword()
        );
        return ResponseEntity.ok(AuthResponse.builder()
            .status(200)
            .message("Password has been reset successfully")
            .build());
    }
} 
