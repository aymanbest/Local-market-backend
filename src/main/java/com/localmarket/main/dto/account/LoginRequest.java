package com.localmarket.main.dto.account;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
} 