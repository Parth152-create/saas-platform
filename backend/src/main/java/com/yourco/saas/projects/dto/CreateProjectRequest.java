package com.yourco.saas.projects.dto;

import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(max = 255, message = "Project name cannot exceed 255 characters")
        String name,

        String description,

        @Size(max = 255, message = "Client name cannot exceed 255 characters")
        String client,

        ProjectStatus status,

        ProjectPriority priority,

        LocalDate startDate,

        LocalDate dueDate,

        BigDecimal budget,

        UUID ownerId,

        List<UUID> memberIds
) {}
