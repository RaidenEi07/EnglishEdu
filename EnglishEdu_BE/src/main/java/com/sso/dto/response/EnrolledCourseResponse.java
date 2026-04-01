package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
public class EnrolledCourseResponse {
    private Long enrollmentId;
    private Long courseId;
    private Integer externalId;
    private String name;
    private String category;
    private String level;
    private String imageUrl;
    private int progress;
    private String status;
    private Instant enrolledAt;
    private Instant lastAccessed;
    private boolean starred;
    private boolean hidden;
    private String teacherNote;
}
