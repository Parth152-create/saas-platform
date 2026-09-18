package com.yourco.saas.hrm.dto;

import java.math.BigDecimal;

public record HrmStatsDto(
        long totalEmployees,
        long activeEmployees,
        long onLeaveEmployees,
        long probationEmployees,
        long inactiveEmployees,
        long totalDepartments,
        BigDecimal averageAttendance,
        int totalBillableHours
) {}
