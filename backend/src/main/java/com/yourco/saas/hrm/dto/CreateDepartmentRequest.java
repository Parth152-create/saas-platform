package com.yourco.saas.hrm.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateDepartmentRequest(
        @NotBlank(message = "Department name is required")
        String name,
        String lead,
        BigDecimal budgetUtilization
) {}
