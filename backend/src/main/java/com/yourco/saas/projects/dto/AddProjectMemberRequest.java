package com.yourco.saas.projects.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddProjectMemberRequest(
        @NotNull(message = "Member ID is required")
        UUID memberId,

        String role
) {}
