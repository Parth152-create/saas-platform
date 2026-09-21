package com.yourco.saas.bulk.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkTaskAssignRequest(
        @NotEmpty(message = "Task IDs must not be empty")
        List<UUID> taskIds,

        @NotNull(message = "Assignee ID is required")
        UUID assigneeId
) {}
