package com.localmarket.main.entity.AccessToken;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "AccessToken")
@Data
public class AccessToken {
    @Id
    private String token;
    private String email;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
} 