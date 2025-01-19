package com.localmarket.main.dto;

import com.localmarket.main.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private Role role = Role.CUSTOMER;
} 