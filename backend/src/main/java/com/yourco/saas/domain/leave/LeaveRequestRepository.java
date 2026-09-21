package com.yourco.saas.domain.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    long countByStatus(LeaveStatus status);

    long countByUserIdAndStatus(UUID userId, LeaveStatus status);

    @Query("SELECT r FROM LeaveRequest r WHERE " +
           "(:userId IS NULL OR r.userId = :userId) AND " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:leaveType IS NULL OR r.leaveType = :leaveType) AND " +
           "(:fromDate IS NULL OR r.endDate >= :fromDate) AND " +
           "(:toDate IS NULL OR r.startDate <= :toDate) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(r.employeeName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.employeeEmail) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.reason) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY r.createdAt DESC")
    List<LeaveRequest> searchRequests(@Param("userId") UUID userId,
                                      @Param("status") LeaveStatus status,
                                      @Param("leaveType") LeaveType leaveType,
                                      @Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate,
                                      @Param("query") String query);

    @Query("SELECT r FROM LeaveRequest r WHERE r.status = 'APPROVED' AND r.startDate <= :toDate AND r.endDate >= :fromDate")
    List<LeaveRequest> findApprovedInDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
}
