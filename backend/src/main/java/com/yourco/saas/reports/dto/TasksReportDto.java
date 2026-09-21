package com.yourco.saas.reports.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TasksReportDto(
        long totalTasks,
        long openTasks,
        long completedTasks,
        long overdueTasks,
        double completionRate,
        Map<String, Long> tasksByStatus,
        Map<String, Long> tasksByPriority,
        List<TaskReportItemDto> tasks
) {
    public record TaskReportItemDto(
            UUID id,
            String title,
            String projectName,
            String status,
            String priority,
            String assigneeName,
            LocalDate dueDate,
            Integer estimatedHours,
            Integer actualHours
    ) {}
}
