package com.yourco.saas.projects.dto;

public record ProjectStatsResponse(
        long activeProjects,
        long totalProjects,
        long openTasks,
        long overdueTasks,
        long completedTasks,
        long myTasks
) {}
