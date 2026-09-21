package com.yourco.saas.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProjectsReportDto(
        long totalProjects,
        long activeProjects,
        long completedProjects,
        long planningProjects,
        long onHoldProjects,
        long archivedProjects,
        Map<String, Long> projectsByStatus,
        Map<String, Long> projectsByPriority,
        List<ProjectReportItemDto> projects
) {
    public record ProjectReportItemDto(
            UUID id,
            String name,
            String client,
            String status,
            String priority,
            LocalDate startDate,
            LocalDate dueDate,
            BigDecimal budget,
            int totalTasks,
            int doneTasks,
            double progressPercentage
    ) {}
}
