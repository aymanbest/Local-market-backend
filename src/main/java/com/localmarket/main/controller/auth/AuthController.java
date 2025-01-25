package com.localmarket.main.controller.auth;

import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.auth.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;

import com.localmarket.main.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.register(request, authHeader));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok().build();
    }
} 