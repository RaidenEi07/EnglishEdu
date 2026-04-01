package com.sso.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.Instant;

@Data
public class TeacherUpdateEnrollmentRequest {

    @Min(0) @Max(100)
    private Integer progress;

    /** pending | active | inprogress | completed | revoked */
    private String status;

    private String teacherNote;

    private Instant expiryDate;
}
