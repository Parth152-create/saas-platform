package com.yourco.saas.reports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LeaveReportDto(
        long totalRequests,
        long pendingRequests,
        long approvedRequests,
        long rejectedRequests,
        long cancelledRequests,
        BigDecimal totalDaysApproved,
        Map<String, Long> requestsByStatus,
        Map<String, Long> requestsByType,
        List<LeaveReportItemDto> requests
) {
    public record LeaveReportItemDto(
            UUID id,
            String employeeName,
            String employeeEmail,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal daysCount,
            String status,
            String reviewerName,
            Instant reviewedAt
    ) {}
}
