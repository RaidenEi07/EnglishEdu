package com.sso.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddCourseTeacherRequest {
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @Pattern(regexp = "PRIMARY|ASSISTANT", message = "Role must be PRIMARY or ASSISTANT")
    private String role = "PRIMARY";
}
