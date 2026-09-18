package com.yourco.saas.hrm.dto;

import com.yourco.saas.domain.hrm.EmployeeStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeRequest(
        @NotBlank(message = "Employee ID is required")
        String employeeId,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Department is required")
        String department,

        @NotBlank(message = "Position is required")
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
