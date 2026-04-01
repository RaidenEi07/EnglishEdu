package com.sso.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateEnrollmentRequest {
    private String status;

    @Min(0) @Max(100)
    private Integer progress;
}
