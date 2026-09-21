package com.yourco.saas.domain.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    List<LeaveBalance> findByUserIdAndYear(UUID userId, int year);

    Optional<LeaveBalance> findByUserIdAndYearAndLeaveType(UUID userId, int year, LeaveType leaveType);

    List<LeaveBalance> findByUserId(UUID userId);
}
