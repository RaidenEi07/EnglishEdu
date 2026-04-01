package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class CourseReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userFullName;
    private String userAvatarUrl;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
