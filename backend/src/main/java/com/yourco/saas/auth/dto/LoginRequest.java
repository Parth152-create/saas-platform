package com.yourco.saas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String tenantId,
        @NotBlank @Email String email,
        @NotBlank String password) {}