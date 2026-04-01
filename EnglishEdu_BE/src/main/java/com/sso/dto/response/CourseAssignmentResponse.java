package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class CourseAssignmentResponse {
    private Long id;
    private Long userId;
    private String username;
    private String studentFullName;
    private Long courseId;
    private String courseName;
    private String courseCategory;
    private String assignedByUsername;
    private Instant assignedAt;
}
