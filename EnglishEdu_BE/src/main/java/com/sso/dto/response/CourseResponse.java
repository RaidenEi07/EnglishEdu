package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private Integer externalId;
    private String name;
    private String category;
    private String level;
    private Long categoryId;
    private Long levelId;
    private String description;
    private String imageUrl;
    private Long teacherId;
    private String teacherName;
    private List<CourseTeacherResponse> teachers;
    private boolean published;
    private boolean free;
    private BigDecimal price;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Long moodleCourseId;
    private java.time.Instant createdAt;
}
