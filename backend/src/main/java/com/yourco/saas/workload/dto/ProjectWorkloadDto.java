package com.yourco.saas.workload.dto;

import java.util.UUID;

public record ProjectWorkloadDto(
        UUID projectId,
        String projectName,
        String status,
        int totalTasks,
        int openTasks,
        int completedTasks,
        int overdueTasks,
        int estimatedHours,
        int actualHours,
        double progressPercentage
) {}
