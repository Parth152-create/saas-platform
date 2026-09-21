package com.yourco.saas.workload.dto;

import java.util.UUID;

public record EmployeeWorkloadDto(
        UUID employeeOrUserId,
        String name,
        String email,
        String department,
        int totalTasks,
        int openTasks,
        int completedTasks,
        int overdueTasks,
        int dueSoonTasks,
        int estimatedHoursTotal,
        int estimatedHoursRemaining,
        int actualHours,
        int weeklyCapacityHours,
        double capacityUtilization,
        String workloadStatus
) {}
