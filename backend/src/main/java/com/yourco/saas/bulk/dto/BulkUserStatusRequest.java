package com.yourco.saas.bulk.dto;

import com.yourco.saas.domain.user.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkUserStatusRequest(
        @NotEmpty(message = "User IDs must not be empty")
        List<UUID> userIds,

        @NotNull(message = "Status is required")
        UserStatus status
) {}
