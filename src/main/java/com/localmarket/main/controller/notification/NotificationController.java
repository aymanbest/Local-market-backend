package com.localmarket.main.controller.notification;

import com.localmarket.main.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.localmarket.main.service.notification.WebSocketService;
import com.localmarket.main.dto.notification.NotificationResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification management APIs")
public class NotificationController {
    private final WebSocketService webSocketService;

    @GetMapping
    @Operation(summary = "Get user's notifications")
    @SecurityRequirement(name = "cookie")
    @ApiResponse(responseCode = "200", description = "Retrieved notifications successfully")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(webSocketService.getStoredNotifications(userDetails.getEmail(), pageable));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get user's unread notification count")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(webSocketService.getUnreadCount(userDetails.getEmail()));
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark all notifications as read")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        webSocketService.markAllAsRead(userDetails.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{notificationId}/mark-read")
    @Operation(summary = "Mark specific notification as read")
    @SecurityRequirement(name = "cookie")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        webSocketService.markAsRead(userDetails.getEmail(), notificationId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    @SecurityRequirement(name = "cookie")
    @ApiResponse(responseCode = "204", description = "Notification deleted successfully")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId) {
        webSocketService.deleteNotification(userDetails.getEmail(), notificationId);
        return ResponseEntity.noContent().build();
    }
} 