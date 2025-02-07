package com.localmarket.main.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthServiceResult {
    private AuthResponse response;
    private String token;
} 