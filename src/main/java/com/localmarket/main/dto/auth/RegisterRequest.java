package com.localmarket.main.dto.auth;


import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String firstname;
    private String lastname;
    private String password;
    // private Role role = Role.CUSTOMER;
} 