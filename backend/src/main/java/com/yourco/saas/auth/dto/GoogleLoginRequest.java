package com.yourco.saas.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String idToken, @NotBlank String tenantId) {}