package com.sso.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLevelRequest {
    @NotBlank(message = "Level name is required")
    private String name;
}
