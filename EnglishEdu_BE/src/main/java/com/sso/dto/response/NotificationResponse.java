package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private String link;
    private String type;
    private boolean read;
    private Instant createdAt;
}
