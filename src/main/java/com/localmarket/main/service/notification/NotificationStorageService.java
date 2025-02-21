package com.localmarket.main.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.localmarket.main.repository.notification.StoredNotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationStorageService {
    private final StoredNotificationRepository storedNotificationRepository;

    @Transactional
    public void markAllAsRead(String email) {
        storedNotificationRepository.markAllAsRead(email);
    }
} 