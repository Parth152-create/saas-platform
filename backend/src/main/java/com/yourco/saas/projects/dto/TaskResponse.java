package com.yourco.saas.projects.dto;

import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        String assigneeName,
        String assigneeEmail,
        LocalDate dueDate,
        Integer estimatedHours,
        Integer actualHours,
        UUID createdById,
        String createdByName,
        Instant createdAt,
        Instant updatedAt
) {
    public static TaskResponse fromEntity(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject() != null ? task.getProject().getId() : null,
                task.getProject() != null ? task.getProject().getName() : null,
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getAssigneeId(),
                task.getAssigneeName(),
                task.getAssigneeEmail(),
                task.getDueDate(),
                task.getEstimatedHours(),
                task.getActualHours(),
                task.getCreatedById(),
                task.getCreatedByName(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
