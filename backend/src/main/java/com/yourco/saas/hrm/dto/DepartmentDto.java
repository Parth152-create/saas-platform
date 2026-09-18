package com.yourco.saas.hrm.dto;

import com.yourco.saas.domain.hrm.Department;

import java.math.BigDecimal;
import java.util.UUID;

public record DepartmentDto(
        UUID id,
        String name,
        String lead,
        BigDecimal budgetUtilization,
        long headCount
) {
    public static DepartmentDto fromEntity(Department d, long headCount) {
        return new DepartmentDto(
                d.getId(),
                d.getName(),
                d.getLead(),
                d.getBudgetUtilization(),
                headCount
        );
    }
}
