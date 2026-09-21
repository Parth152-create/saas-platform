package com.yourco.saas.leave.dto;

import com.yourco.saas.domain.leave.LeaveBalance;
import com.yourco.saas.domain.leave.LeaveType;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveBalanceResponse(
        UUID id,
        UUID userId,
        int year,
        LeaveType leaveType,
        BigDecimal totalDays,
        BigDecimal usedDays,
        BigDecimal pendingDays,
        BigDecimal remainingDays
) {
    public static LeaveBalanceResponse fromEntity(LeaveBalance entity) {
        return new LeaveBalanceResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getYear(),
                entity.getLeaveType(),
                entity.getTotalDays(),
                entity.getUsedDays(),
                entity.getPendingDays(),
                entity.getRemainingDays()
        );
    }
}
