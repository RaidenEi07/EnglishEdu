package com.sso.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank(message = "Course name is required")
    private String name;

    private String category;
    private String level;
    private Long categoryId;
    private Long levelId;
    private String description;
    private String imageUrl;
    private Long teacherId;
    private Integer externalId;
    private boolean published = true;
    private boolean free = true;
    private java.math.BigDecimal price;
}
