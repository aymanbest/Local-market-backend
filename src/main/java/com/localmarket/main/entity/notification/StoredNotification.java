package com.localmarket.main.entity.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.LocalDateTime;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;

@Entity
@Data
@Table(name = "StoredNotification")
public class StoredNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    @Column(columnDefinition = "TEXT")
    private String data;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
} 