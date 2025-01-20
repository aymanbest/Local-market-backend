package com.localmarket.main.controller.auth;

import com.localmarket.main.dto.auth.AuthResponse;
import com.localmarket.main.dto.account.RegisterRequest;
import com.localmarket.main.dto.auth.AuthRequest;

import com.localmarket.main.service.auth.AuthService;
import com.localmarket.main.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenBlacklist tokenBlacklist;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7);
        tokenBlacklist.blacklist(jwt);
        return ResponseEntity.ok().build();
    }
} 