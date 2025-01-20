package com.localmarket.main.dto.user;

import lombok.Data;

@Data
public class AccountCreationRequest {
    private boolean createAccount;
    private String username;
    private String password;
} 