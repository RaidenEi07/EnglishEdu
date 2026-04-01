package com.sso.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TeacherDashboardResponse {
    private long totalCourses;
    private long totalStudents;
    private long pendingStudents;
    private long activeStudents;
}
