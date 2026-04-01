package com.sso.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotBlank
    @Pattern(regexp = "STUDENT|TEACHER|ADMIN", message = "Role must be STUDENT, TEACHER, or ADMIN")
    private String role;
}
