package com.yourco.saas.workload.dto;

import java.util.List;

public record WorkforceWorkloadSummaryDto(
        int totalEmployees,
        int totalTasks,
        int openTasks,
        int completedTasks,
        int overdueTasks,
        int dueSoonTasks,
        int totalEstimatedHours,
        int totalActualHours,
        int standardWeeklyCapacityHours,
        double averageUtilizationPercentage,
        List<EmployeeWorkloadDto> employeeWorkloads,
        List<DepartmentWorkloadDto> departmentWorkloads,
        List<ProjectWorkloadDto> projectWorkloads
) {}
