package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CourseTeacherResponse {
    private Long id;
    private Long teacherId;
    private String teacherUsername;
    private String teacherFullName;
    private String role;  // PRIMARY, ASSISTANT
}
