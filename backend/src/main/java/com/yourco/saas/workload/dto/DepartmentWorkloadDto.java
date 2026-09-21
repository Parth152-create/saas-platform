package com.yourco.saas.workload.dto;

public record DepartmentWorkloadDto(
        String department,
        int employeeCount,
        int totalTasks,
        int openTasks,
        int overdueTasks,
        int totalEstimatedHours,
        int totalActualHours,
        double averageUtilization
) {}
