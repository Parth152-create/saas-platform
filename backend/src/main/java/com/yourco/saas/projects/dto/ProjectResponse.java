package com.yourco.saas.projects.dto;

import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String client,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate startDate,
        LocalDate dueDate,
        BigDecimal budget,
        UUID ownerId,
        String ownerName,
        int progress,
        int totalTasks,
        int doneTasks,
        int overdueTasks,
        int teamMemberCount,
        List<ProjectMemberDto> members,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse fromEntity(Project project, int totalTasks, int doneTasks, int overdueTasks, List<ProjectMemberDto> members) {
        int progress = totalTasks == 0 ? 0 : (int) Math.round(((double) doneTasks / totalTasks) * 100.0);
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getClient(),
                project.getStatus(),
                project.getPriority(),
                project.getStartDate(),
                project.getDueDate(),
                project.getBudget(),
                project.getOwnerId(),
                project.getOwnerName(),
                progress,
                totalTasks,
                doneTasks,
                overdueTasks,
                members != null ? members.size() : 0,
                members != null ? members : List.of(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
