package com.yourco.saas.leave.dto;

import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestResponse(
        UUID id,
        UUID userId,
        UUID employeeId,
        String employeeName,
        String employeeEmail,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal daysCount,
        String reason,
        LeaveStatus status,
        UUID reviewerId,
        String reviewerName,
        String reviewNote,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static LeaveRequestResponse fromEntity(LeaveRequest entity) {
        return new LeaveRequestResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getEmployeeId(),
                entity.getEmployeeName(),
                entity.getEmployeeEmail(),
                entity.getLeaveType(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getDaysCount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getReviewerId(),
                entity.getReviewerName(),
                entity.getReviewNote(),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
