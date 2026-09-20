package com.yourco.saas.projects.dto;

import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(max = 255, message = "Task title cannot exceed 255 characters")
        String title,

        String description,

        TaskStatus status,

        TaskPriority priority,

        UUID assigneeId,

        LocalDate dueDate,

        Integer estimatedHours,

        Integer actualHours
) {}
