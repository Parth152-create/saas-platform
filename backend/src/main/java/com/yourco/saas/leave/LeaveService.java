package com.yourco.saas.leave;

import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.collaboration.NotificationType;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.leave.dto.CreateLeaveRequest;
import com.yourco.saas.leave.dto.LeaveBalanceResponse;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.leave.dto.LeaveReviewRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.yourco.saas.collaboration.service.NotificationService notificationService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        UserRepository userRepository,
                        EmployeeRepository employeeRepository,
                        AuditLogRepository auditLogRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getMyLeaveRequests() {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(LeaveRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyLeaveBalances() {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        int currentYear = LocalDate.now().getYear();
        ensureDefaultBalances(actorId, currentYear);
        return leaveBalanceRepository.findByUserIdAndYear(actorId, currentYear)
                .stream()
                .map(LeaveBalanceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAllLeaveRequests(LeaveStatus status, LeaveType type,
                                                          LocalDate fromDate, LocalDate toDate, String query) {
        validateManagerOrAbove();
        return leaveRequestRepository.searchRequests(null, status, type, fromDate, toDate, query)
                .stream()
                .map(LeaveRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingLeaveRequests() {
        validateManagerOrAbove();
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveStatus.PENDING)
                .stream()
                .map(LeaveRequestResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequestById(UUID id) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (actorRole == Role.USER && !request.getUserId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this leave request");
        }

        return LeaveRequestResponse.fromEntity(request);
    }

    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest req) {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disabled or inactive users cannot create leave requests");
        }

        if (req.startDate().isAfter(req.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date");
        }

        long days = ChronoUnit.DAYS.between(req.startDate(), req.endDate()) + 1;
        BigDecimal daysCount = BigDecimal.valueOf(Math.max(1, days));

        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(user.getEmail());
        UUID employeeId = empOpt.map(Employee::getId).orElse(null);
        String employeeName = empOpt.map(Employee::getName).orElse(user.getEmail().split("@")[0]);

        LeaveRequest leaveRequest = new LeaveRequest(
                actorId,
                employeeId,
                employeeName,
                user.getEmail(),
                req.leaveType(),
                req.startDate(),
                req.endDate(),
                daysCount,
                req.reason().trim(),
                LeaveStatus.PENDING
        );

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        // Update pending balance
        int year = req.startDate().getYear();
        ensureDefaultBalances(actorId, year);
        leaveBalanceRepository.findByUserIdAndYearAndLeaveType(actorId, year, req.leaveType())
                .ifPresent(balance -> {
                    balance.setPendingDays(balance.getPendingDays().add(daysCount));
                    leaveBalanceRepository.save(balance);
                });

        // Audit log
        auditLogRepository.save(new AuditLog(
                actorId,
                user.getRole().name(),
                "LEAVE_CREATED",
                "SUCCESS",
                "request_id=" + saved.getId() + ", type=" + saved.getLeaveType() +
                        ", days=" + daysCount + ", start=" + saved.getStartDate() + ", end=" + saved.getEndDate()
        ));

        // Notify managers and admins
        if (notificationService != null) {
            List<User> managersAndAdmins = userRepository.findAll().stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .filter(u -> u.getRole() == Role.MANAGER || u.getRole() == Role.ADMIN || u.getRole() == Role.SUPER_ADMIN)
                    .filter(u -> !u.getId().equals(actorId))
                    .toList();

            for (User mgr : managersAndAdmins) {
                try {
                    notificationService.createNotification(
                            mgr.getId(),
                            NotificationType.LEAVE_SUBMITTED,
                            "New Leave Request: " + employeeName,
                            employeeName + " requested " + daysCount + " day(s) of " + req.leaveType() +
                                    " leave from " + req.startDate() + " to " + req.endDate(),
                            "LEAVE_REQUEST",
                            saved.getId().toString(),
                            "/app/leave"
                    );
                } catch (Exception e) {
                    log.warn("Failed to notify manager {} of leave request: {}", mgr.getId(), e.getMessage());
                }
            }
        }

        return LeaveRequestResponse.fromEntity(saved);
    }

    public LeaveRequestResponse cancelLeaveRequest(UUID id) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (!request.getUserId().equals(actorId) && actorRole != Role.ADMIN && actorRole != Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only cancel your own leave requests");
        }

        if (request.getStatus() != LeaveStatus.PENDING && request.getStatus() != LeaveStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel a request that is already " + request.getStatus());
        }

        LeaveStatus prevStatus = request.getStatus();
        request.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(request);

        if (prevStatus == LeaveStatus.PENDING) {
            int year = request.getStartDate().getYear();
            leaveBalanceRepository.findByUserIdAndYearAndLeaveType(request.getUserId(), year, request.getLeaveType())
                    .ifPresent(balance -> {
                        BigDecimal newPending = balance.getPendingDays().subtract(request.getDaysCount());
                        balance.setPendingDays(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
                        leaveBalanceRepository.save(balance);
                    });
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "LEAVE_CANCELLED",
                "SUCCESS",
                "request_id=" + saved.getId()
        ));

        return LeaveRequestResponse.fromEntity(saved);
    }

    public LeaveRequestResponse approveLeaveRequest(UUID id, LeaveReviewRequest reviewReq) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        // USER CANNOT APPROVE THEIR OWN REQUEST!
        if (actorId != null && actorId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot approve their own leave request");
        }

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot approve a request that is not PENDING");
        }

        String reviewerName = currentActorName();
        request.setStatus(LeaveStatus.APPROVED);
        request.setReviewerId(actorId);
        request.setReviewerName(reviewerName);
        request.setReviewedAt(Instant.now());
        if (reviewReq != null && reviewReq.reviewNote() != null) {
            request.setReviewNote(reviewReq.reviewNote().trim());
        }

        LeaveRequest saved = leaveRequestRepository.save(request);

        // Adjust leave balance: move from pending to used
        int year = request.getStartDate().getYear();
        leaveBalanceRepository.findByUserIdAndYearAndLeaveType(request.getUserId(), year, request.getLeaveType())
                .ifPresent(balance -> {
                    BigDecimal newPending = balance.getPendingDays().subtract(request.getDaysCount());
                    balance.setPendingDays(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
                    balance.setUsedDays(balance.getUsedDays().add(request.getDaysCount()));
                    leaveBalanceRepository.save(balance);
                });

        // If leave spans today, update employee status to ON_LEAVE
        LocalDate today = LocalDate.now();
        if (request.getEmployeeId() != null && !today.isBefore(request.getStartDate()) && !today.isAfter(request.getEndDate())) {
            employeeRepository.findById(request.getEmployeeId()).ifPresent(emp -> {
                emp.setStatus(EmployeeStatus.ON_LEAVE);
                employeeRepository.save(emp);
            });
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "LEAVE_APPROVED",
                "SUCCESS",
                "request_id=" + saved.getId() + ", employee_id=" + saved.getEmployeeId() +
                        ", reviewer_id=" + actorId
        ));

        // Notify employee
        if (notificationService != null) {
            try {
                notificationService.createNotification(
                        saved.getUserId(),
                        NotificationType.LEAVE_APPROVED,
                        "Leave Request Approved",
                        "Your " + saved.getLeaveType() + " leave request for " + saved.getDaysCount() +
                                " day(s) starting " + saved.getStartDate() + " has been approved by " + reviewerName,
                        "LEAVE_REQUEST",
                        saved.getId().toString(),
                        "/app/leave"
                );
            } catch (Exception e) {
                log.warn("Failed to notify user {} of approved leave: {}", saved.getUserId(), e.getMessage());
            }
        }

        return LeaveRequestResponse.fromEntity(saved);
    }

    public LeaveRequestResponse rejectLeaveRequest(UUID id, LeaveReviewRequest reviewReq) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        // USER CANNOT REJECT THEIR OWN REQUEST AS MANAGER
        if (actorId != null && actorId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot review their own leave request");
        }

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reject a request that is not PENDING");
        }

        String reviewerName = currentActorName();
        request.setStatus(LeaveStatus.REJECTED);
        request.setReviewerId(actorId);
        request.setReviewerName(reviewerName);
        request.setReviewedAt(Instant.now());
        if (reviewReq != null && reviewReq.reviewNote() != null) {
            request.setReviewNote(reviewReq.reviewNote().trim());
        }

        LeaveRequest saved = leaveRequestRepository.save(request);

        // Adjust leave balance: remove from pending
        int year = request.getStartDate().getYear();
        leaveBalanceRepository.findByUserIdAndYearAndLeaveType(request.getUserId(), year, request.getLeaveType())
                .ifPresent(balance -> {
                    BigDecimal newPending = balance.getPendingDays().subtract(request.getDaysCount());
                    balance.setPendingDays(newPending.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newPending);
                    leaveBalanceRepository.save(balance);
                });

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "LEAVE_REJECTED",
                "SUCCESS",
                "request_id=" + saved.getId() + ", employee_id=" + saved.getEmployeeId() +
                        ", reviewer_id=" + actorId + ", reason=" + request.getReviewNote()
        ));

        // Notify employee
        if (notificationService != null) {
            try {
                notificationService.createNotification(
                        saved.getUserId(),
                        NotificationType.LEAVE_REJECTED,
                        "Leave Request Rejected",
                        "Your " + saved.getLeaveType() + " leave request for " + saved.getDaysCount() +
                                " day(s) starting " + saved.getStartDate() + " was rejected by " + reviewerName +
                                (saved.getReviewNote() != null ? ": " + saved.getReviewNote() : ""),
                        "LEAVE_REQUEST",
                        saved.getId().toString(),
                        "/app/leave"
                );
            } catch (Exception e) {
                log.warn("Failed to notify user {} of rejected leave: {}", saved.getUserId(), e.getMessage());
            }
        }

        return LeaveRequestResponse.fromEntity(saved);
    }

    private void ensureDefaultBalances(UUID userId, int year) {
        List<LeaveBalance> existing = leaveBalanceRepository.findByUserIdAndYear(userId, year);
        if (existing.isEmpty()) {
            leaveBalanceRepository.save(new LeaveBalance(userId, year, LeaveType.ANNUAL, new BigDecimal("20.0")));
            leaveBalanceRepository.save(new LeaveBalance(userId, year, LeaveType.SICK, new BigDecimal("10.0")));
            leaveBalanceRepository.save(new LeaveBalance(userId, year, LeaveType.CASUAL, new BigDecimal("5.0")));
        }
    }

    private void validateManagerOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions: Requires MANAGER or above");
        }
    }

    private UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Role currentActorRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String authority = ga.getAuthority();
            if (authority.startsWith("ROLE_")) {
                try {
                    return Role.valueOf(authority.substring(5));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }

    private String currentActorName() {
        UUID actorId = currentActorId();
        if (actorId == null) return "System";
        return userRepository.findById(actorId)
                .map(User::getEmail)
                .flatMap(employeeRepository::findByEmailIgnoreCase)
                .map(Employee::getName)
                .orElseGet(() -> userRepository.findById(actorId).map(User::getEmail).orElse("User"));
    }
}
