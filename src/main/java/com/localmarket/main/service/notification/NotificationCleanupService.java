package com.localmarket.main.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import com.localmarket.main.repository.notification.StoredNotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationCleanupService {
    private final StoredNotificationRepository storedNotificationRepository;

    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    @Transactional
    public void cleanupOldNotifications() {
        storedNotificationRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
} 