package com.sso.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignCoursesRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "courseIds is required")
    private List<Long> courseIds;
}
