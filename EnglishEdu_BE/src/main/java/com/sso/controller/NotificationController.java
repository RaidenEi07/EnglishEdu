package com.sso.controller;

import com.sso.dto.response.ApiResponse;
import com.sso.dto.response.NotificationResponse;
import com.sso.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUnreadCount(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getNotifications(userId)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails user) {
        Long userId = Long.parseLong(user.getUsername());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        Long userId = Long.parseLong(user.getUsername());
        notificationService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted"));
    }
}
