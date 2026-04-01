package com.sso.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateAdminUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username may only contain letters, digits, underscores, dots and hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    private String firstName;
    private String lastName;

    @Pattern(regexp = "^(STUDENT|TEACHER|ADMIN)$", message = "Role must be STUDENT, TEACHER, or ADMIN")
    private String role = "STUDENT";
}
