package com.yourco.saas.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank String tenantId,
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password
) {}