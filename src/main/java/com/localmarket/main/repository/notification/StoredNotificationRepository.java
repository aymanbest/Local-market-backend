package com.localmarket.main.repository.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.localmarket.main.entity.notification.StoredNotification;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoredNotificationRepository extends JpaRepository<StoredNotification, Long> {
    List<StoredNotification> findByRecipientEmailAndReadFalseAndExpiresAtGreaterThan(
        String recipientEmail, 
        LocalDateTime now
    );
    
    Page<StoredNotification> findByRecipientEmailOrderByTimestampDesc(
        String recipientEmail, 
        Pageable pageable
    );
    
    Long countByRecipientEmailAndReadFalse(String recipientEmail);
    
    @Modifying
    @Query("UPDATE StoredNotification n SET n.read = true WHERE n.recipientEmail = :email")
    void markAllAsRead(String email);
    
    @Modifying
    @Query("DELETE FROM StoredNotification n WHERE n.expiresAt < :date")
    void deleteByExpiresAtBefore(LocalDateTime date);
} 