package com.localmarket.main.dto.auth;

import com.localmarket.main.entity.user.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private String password;
    private Role role;
} 