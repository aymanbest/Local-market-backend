package com.localmarket.main.dto.auth;

import lombok.Data;

@Data
public class PasswordResetVerifyRequest {
    private String email;
    private String code;
    private String newPassword;
} 