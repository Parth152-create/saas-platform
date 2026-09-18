package com.yourco.saas.hrm.dto;

import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDto(
        UUID id,
        String employeeId,
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
        Integer billableHours,
        Instant createdAt,
        Instant updatedAt
) {
    public static EmployeeDto fromEntity(Employee e) {
        return new EmployeeDto(
                e.getId(),
                e.getEmployeeId(),
                e.getName(),
                e.getEmail(),
                e.getDepartment(),
                e.getPosition(),
                e.getStatus(),
                e.getHireDate(),
                e.getPhone(),
                e.getWorkModel(),
                e.getLocation(),
                e.getManager(),
                e.getAvatarColor(),
                e.getAttendanceRate(),
                e.getBillableHours(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
