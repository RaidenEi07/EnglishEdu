package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String avatarUrl;
    private String role;
    private boolean active;
    private boolean guest;
    private Long moodleId;
    private Instant createdAt;
    private Instant updatedAt;
}
