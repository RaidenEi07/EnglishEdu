package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class AdminEnrollmentResponse {
    private Long id;

    private Long courseId;
    private String courseName;

    private Long studentId;
    private String studentUsername;
    private String studentFirstName;
    private String studentLastName;
    private String studentFullName;

    private String status;
    private Integer progress;

    private Instant requestDate;
    private Instant enrolledAt;
    private Instant approvedAt;
    private String approvedByUsername;
    private String teacherNote;
    private Instant expiryDate;
}
