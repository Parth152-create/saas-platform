package com.yourco.saas.bulk.dto;

import com.yourco.saas.domain.projects.TaskStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record BulkTaskStatusRequest(
        @NotEmpty(message = "Task IDs must not be empty")
        List<UUID> taskIds,

        @NotNull(message = "Status is required")
        TaskStatus status
) {}
