package com.yourco.saas.bulk;

import com.yourco.saas.bulk.dto.BulkItemErrorDto;
import com.yourco.saas.bulk.dto.BulkOperationResultDto;
import com.yourco.saas.bulk.dto.BulkTaskAssignRequest;
import com.yourco.saas.bulk.dto.BulkTaskStatusRequest;
import com.yourco.saas.bulk.dto.BulkUserStatusRequest;
import com.yourco.saas.collaboration.service.NotificationService;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.collaboration.NotificationType;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.projects.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BulkOperationService {

    private static final Logger log = LoggerFactory.getLogger(BulkOperationService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectService projectService;
    private final AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationService notificationService;

    public BulkOperationService(TaskRepository taskRepository,
                                UserRepository userRepository,
                                EmployeeRepository employeeRepository,
                                ProjectService projectService,
                                AuditLogRepository auditLogRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.projectService = projectService;
        this.auditLogRepository = auditLogRepository;
    }

    public BulkOperationResultDto bulkUpdateTaskStatus(BulkTaskStatusRequest req) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        List<UUID> successIds = new ArrayList<>();
        List<BulkItemErrorDto> errors = new ArrayList<>();

        String actorEmail = getActorEmail(actorId);

        for (UUID taskId : req.taskIds()) {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                errors.add(new BulkItemErrorDto(taskId, "Task not found"));
                continue;
            }

            Task task = taskOpt.get();

            // Regular user authorization check: can only update assigned tasks
            if (actorRole == Role.USER) {
                boolean isAssignee = (task.getAssigneeId() != null && task.getAssigneeId().equals(actorId))
                        || (task.getAssigneeEmail() != null && task.getAssigneeEmail().equalsIgnoreCase(actorEmail));
                if (!isAssignee) {
                    errors.add(new BulkItemErrorDto(taskId, "Unauthorized: Task not assigned to user"));
                    continue;
                }
            }

            task.setStatus(req.status());
            taskRepository.save(task);
            successIds.add(taskId);
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "BULK_TASKS_STATUS",
                successIds.isEmpty() ? "FAILURE" : "SUCCESS",
                "status=" + req.status() + ",success=" + successIds.size() + ",failures=" + errors.size()
        ));

        return new BulkOperationResultDto(
                req.taskIds().size(),
                successIds.size(),
                errors.size(),
                successIds,
                errors
        );
    }

    public BulkOperationResultDto bulkAssignTasks(BulkTaskAssignRequest req) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        ProjectService.ResolvedUser resolved = projectService.resolveUserOrEmployee(req.assigneeId());
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee does not belong to the current workspace");
        }

        List<UUID> successIds = new ArrayList<>();
        List<BulkItemErrorDto> errors = new ArrayList<>();

        for (UUID taskId : req.taskIds()) {
            Optional<Task> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isEmpty()) {
                errors.add(new BulkItemErrorDto(taskId, "Task not found"));
                continue;
            }

            Task task = taskOpt.get();
            task.setAssigneeId(req.assigneeId());
            task.setAssigneeName(resolved.name());
            task.setAssigneeEmail(resolved.email());
            taskRepository.save(task);
            successIds.add(taskId);
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "BULK_TASKS_ASSIGNED",
                successIds.isEmpty() ? "FAILURE" : "SUCCESS",
                "assignee_id=" + req.assigneeId() + ",success=" + successIds.size() + ",failures=" + errors.size()
        ));

        // Send consolidated notification to assignee
        if (!successIds.isEmpty() && notificationService != null) {
            UUID targetUserId = projectService.resolveUserId(req.assigneeId());
            if (targetUserId != null && !targetUserId.equals(actorId)) {
                try {
                    notificationService.createNotification(
                            targetUserId,
                            NotificationType.TASK_ASSIGNED,
                            "Bulk Tasks Assigned",
                            "You were assigned " + successIds.size() + " tasks in your workspace",
                            "TASKS",
                            req.assigneeId().toString(),
                            "/app/tasks"
                    );
                } catch (Exception e) {
                    log.warn("Failed to notify assignee of bulk task assignment: {}", e.getMessage());
                }
            }
        }

        return new BulkOperationResultDto(
                req.taskIds().size(),
                successIds.size(),
                errors.size(),
                successIds,
                errors
        );
    }

    public BulkOperationResultDto bulkUpdateUserStatus(BulkUserStatusRequest req) {
        validateAdminOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        List<UUID> successIds = new ArrayList<>();
        List<BulkItemErrorDto> errors = new ArrayList<>();

        for (UUID userId : req.userIds()) {
            // Cannot deactivate oneself
            if (actorId != null && actorId.equals(userId) && req.status() == UserStatus.DISABLED) {
                errors.add(new BulkItemErrorDto(userId, "Cannot deactivate current user"));
                continue;
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                errors.add(new BulkItemErrorDto(userId, "User not found"));
                continue;
            }

            User targetUser = userOpt.get();

            // ADMIN cannot deactivate SUPER_ADMIN
            if (targetUser.getRole() == Role.SUPER_ADMIN && actorRole != Role.SUPER_ADMIN) {
                errors.add(new BulkItemErrorDto(userId, "Cannot modify SUPER_ADMIN status"));
                continue;
            }

            targetUser.setStatus(req.status());
            userRepository.save(targetUser);

            // Update matching employee status
            employeeRepository.findByEmailIgnoreCase(targetUser.getEmail()).ifPresent(emp -> {
                if (req.status() == UserStatus.DISABLED) {
                    emp.setStatus(EmployeeStatus.INACTIVE);
                } else if (req.status() == UserStatus.ACTIVE && emp.getStatus() == EmployeeStatus.INACTIVE) {
                    emp.setStatus(EmployeeStatus.ACTIVE);
                }
                employeeRepository.save(emp);
            });

            successIds.add(userId);
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "BULK_USERS_STATUS",
                successIds.isEmpty() ? "FAILURE" : "SUCCESS",
                "status=" + req.status() + ",success=" + successIds.size() + ",failures=" + errors.size()
        ));

        return new BulkOperationResultDto(
                req.userIds().size(),
                successIds.size(),
                errors.size(),
                successIds,
                errors
        );
    }

    private void validateManagerOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires MANAGER or above");
        }
    }

    private void validateAdminOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requires ADMIN or above");
        }
    }

    private String getActorEmail(UUID actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId).map(User::getEmail).orElse(null);
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
}
