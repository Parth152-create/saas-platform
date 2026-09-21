package com.yourco.saas.selfservice.dto;

import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SelfServiceProfileDto(
        UUID userId,
        String email,
        Role role,
        String tenantId,
        UUID employeeId,
        String employeeCode,
        String name,
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
) {
    public static SelfServiceProfileDto from(User user, Employee employee, String tenantId) {
        if (employee != null) {
            return new SelfServiceProfileDto(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    tenantId,
                    employee.getId(),
                    employee.getEmployeeId(),
                    employee.getName(),
                    employee.getDepartment(),
                    employee.getPosition(),
                    employee.getStatus(),
                    employee.getHireDate(),
                    employee.getPhone(),
                    employee.getWorkModel(),
                    employee.getLocation(),
                    employee.getManager(),
                    employee.getAvatarColor(),
                    employee.getAttendanceRate(),
                    employee.getBillableHours()
            );
        } else {
            return new SelfServiceProfileDto(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    tenantId,
                    null,
                    null,
                    user.getEmail().split("@")[0],
                    "General",
                    user.getRole().name(),
                    EmployeeStatus.ACTIVE,
                    LocalDate.now(),
                    null,
                    "Remote",
                    null,
                    null,
                    "bg-zinc-800 text-zinc-100",
                    new BigDecimal("100.00"),
                    0
            );
        }
    }
}
