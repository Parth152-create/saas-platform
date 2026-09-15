package com.yourco.saas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9_-]{0,49}$", message = "must be lowercase letters, numbers, hyphens or underscores, starting with a letter")
        String tenantId,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "must be at least 8 characters")
        String password
) {}