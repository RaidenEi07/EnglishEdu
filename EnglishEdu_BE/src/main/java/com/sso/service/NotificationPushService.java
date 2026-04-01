package com.sso.service;

import com.sso.dto.response.NotificationResponse;
import com.sso.entity.Notification;
import com.sso.entity.User;
import com.sso.repository.NotificationRepository;
import com.sso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create a notification in DB and push it via WebSocket to the user.
     */
    @Transactional
    public void sendNotification(Long userId, String message, String link, String type) {
        User user = userRepository.getReferenceById(userId);

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .type(type != null ? type : "GENERAL")
                .build();
        notification = notificationRepository.save(notification);

        // Push via WebSocket
        NotificationResponse response = NotificationResponse.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .link(notification.getLink())
                .type(notification.getType())
                .read(false)
                .createdAt(notification.getCreatedAt())
                .build();

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                response
        );
    }
}
