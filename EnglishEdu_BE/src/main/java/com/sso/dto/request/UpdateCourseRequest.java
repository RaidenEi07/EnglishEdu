package com.sso.dto.request;

import lombok.Data;

@Data
public class UpdateCourseRequest {

    private String name;
    private String category;
    private String level;
    private Long categoryId;
    private Long levelId;
    private String description;
    private String imageUrl;
    /** Set to 0 to unassign the current teacher. */
    private Long teacherId;
    private Boolean published;
    private Boolean free;
    private java.math.BigDecimal price;
}
