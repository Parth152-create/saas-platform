package com.yourco.saas.reports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkforceReportDto(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long onLeaveEmployees,
        long probationEmployees,
        Map<String, Long> roleDistribution,
        Map<String, Long> departmentDistribution,
        List<EmployeeReportItemDto> employees
) {
    public record EmployeeReportItemDto(
            UUID id,
            String employeeId,
            String name,
            String email,
            String department,
            String position,
            String status,
            LocalDate hireDate,
            BigDecimal attendanceRate,
            Integer billableHours
    ) {}
}
