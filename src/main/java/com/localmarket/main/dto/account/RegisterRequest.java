package com.localmarket.main.dto.account;

import com.localmarket.main.entity.user.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Role role = Role.CUSTOMER;
} 