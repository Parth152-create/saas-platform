package com.yourco.saas.hrm.dto;

import com.yourco.saas.domain.hrm.EmployeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        String name,
        String email,
        String department,
        String position,
        EmployeeStatus status,
        LocalDate hireDate,
        String phone,
        String workModel,
        String location,
        String manager,
        String avatarColor,
        BigDecimal attendanceRate,
        Integer billableHours
) {}
