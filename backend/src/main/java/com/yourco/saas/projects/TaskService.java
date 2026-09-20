package com.yourco.saas.projects;

import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.projects.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.projects.dto.CreateTaskRequest;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.projects.dto.UpdateTaskRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.yourco.saas.collaboration.service.NotificationService notificationService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       ProjectService projectService,
                       UserRepository userRepository,
                       AuditLogRepository auditLogRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(UUID projectId, TaskStatus status, TaskPriority priority,
                                                UUID assigneeId, Boolean isOverdue, String search) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        List<Task> tasks = taskRepository.searchTasks(projectId, status, priority, assigneeId, isOverdue, LocalDate.now(), search);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(UUID projectId, TaskStatus status, TaskPriority priority,
                                         UUID assigneeId, Boolean isOverdue, String search) {
        List<Task> tasks = taskRepository.searchTasks(projectId, status, priority, assigneeId, isOverdue, LocalDate.now(), search);
        return tasks.stream().map(TaskResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        return TaskResponse.fromEntity(task);
    }

    public TaskResponse createTask(UUID projectId, CreateTaskRequest request) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        String assigneeName = null;
        String assigneeEmail = null;
        if (request.assigneeId() != null) {
            ProjectService.ResolvedUser res = projectService.resolveUserOrEmployee(request.assigneeId());
            if (res == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must belong to the current workspace");
            }
            assigneeName = res.name();
            assigneeEmail = res.email();
        }

        String createdByName = "User";
        if (actorId != null) {
            ProjectService.ResolvedUser actorRes = projectService.resolveUserOrEmployee(actorId);
            if (actorRes != null) {
                createdByName = actorRes.name();
            }
        }

        Task task = new Task(
                project,
                request.title().trim(),
                request.description(),
                request.status() != null ? request.status() : TaskStatus.TODO,
                request.priority() != null ? request.priority() : TaskPriority.MEDIUM,
                request.assigneeId(),
                assigneeName,
                assigneeEmail,
                request.dueDate(),
                request.estimatedHours() != null ? request.estimatedHours() : 0,
                request.actualHours() != null ? request.actualHours() : 0,
                actorId,
                createdByName
        );

        Task saved = taskRepository.save(task);

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "TASK_CREATED",
                "SUCCESS",
                "task_id=" + saved.getId() + ", project_id=" + project.getId() + ", title=" + saved.getTitle()
        ));

        if (saved.getAssigneeId() != null) {
            auditLogRepository.save(new AuditLog(
                    actorId,
                    actorRole != null ? actorRole.name() : null,
                    "TASK_ASSIGNED",
                    "SUCCESS",
                    "task_id=" + saved.getId() + ", project_id=" + project.getId() + ", assignee_id=" + saved.getAssigneeId()
            ));

            if (notificationService != null) {
                UUID targetUserId = resolveUserId(saved.getAssigneeId());
                if (targetUserId != null && !targetUserId.equals(actorId)) {
                    try {
                        notificationService.createNotification(
                                targetUserId,
                                com.yourco.saas.domain.collaboration.NotificationType.TASK_ASSIGNED,
                                "Task Assigned: " + saved.getTitle(),
                                createdByName + " assigned you the task '" + saved.getTitle() + "' in project " + project.getName(),
                                "TASK",
                                saved.getId().toString(),
                                "/app/tasks"
                        );
                    } catch (Exception e) {
                        log.warn("Failed to create task assigned notification: {}", e.getMessage());
                    }
                }
            }
        }

        return TaskResponse.fromEntity(saved);
    }

    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        boolean isRegularUser = (actorRole == Role.USER);

        if (isRegularUser) {
            // Regular user restrictions
            if (request.title() != null || request.description() != null || request.priority() != null
                    || request.assigneeId() != null || request.dueDate() != null || request.estimatedHours() != null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Regular users may only update task progress and status");
            }

            // User must be assignee or task unassigned
            if (task.getAssigneeId() != null && !task.getAssigneeId().equals(actorId)) {
                // Check if user's email matches assignee's email
                String actorEmail = getActorEmail(actorId);
                if (actorEmail == null || !actorEmail.equalsIgnoreCase(task.getAssigneeEmail())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users can only update their own assigned tasks");
                }
            }
        }

        TaskStatus oldStatus = task.getStatus();
        UUID oldAssigneeId = task.getAssigneeId();

        if (request.title() != null && !request.title().isBlank()) {
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(request.estimatedHours());
        }
        if (request.actualHours() != null) {
            task.setActualHours(request.actualHours());
        }

        if (request.status() != null) {
            task.setStatus(request.status());
        }

        if (!isRegularUser && request.assigneeId() != null && !request.assigneeId().equals(oldAssigneeId)) {
            ProjectService.ResolvedUser res = projectService.resolveUserOrEmployee(request.assigneeId());
            if (res == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignee must belong to the current workspace");
            }
            task.setAssigneeId(request.assigneeId());
            task.setAssigneeName(res.name());
            task.setAssigneeEmail(res.email());
        }

        Task saved = taskRepository.save(task);

        // Audit logs
        if (request.status() != null && request.status() != oldStatus) {
            auditLogRepository.save(new AuditLog(
                    actorId,
                    actorRole != null ? actorRole.name() : null,
                    "TASK_STATUS_CHANGED",
                    "SUCCESS",
                    "task_id=" + saved.getId() + ", project_id=" + saved.getProject().getId() +
                            ", from=" + oldStatus + ", to=" + request.status()
            ));

            if (request.status() == TaskStatus.DONE) {
                auditLogRepository.save(new AuditLog(
                        actorId,
                        actorRole != null ? actorRole.name() : null,
                        "TASK_COMPLETED",
                        "SUCCESS",
                        "task_id=" + saved.getId() + ", project_id=" + saved.getProject().getId()
                ));

                if (notificationService != null && saved.getCreatedById() != null) {
                    UUID targetUserId = resolveUserId(saved.getCreatedById());
                    if (targetUserId != null && !targetUserId.equals(actorId)) {
                        try {
                            notificationService.createNotification(
                                    targetUserId,
                                    com.yourco.saas.domain.collaboration.NotificationType.TASK_COMPLETED,
                                    "Task Completed: " + saved.getTitle(),
                                    "Task '" + saved.getTitle() + "' in project " + saved.getProject().getName() + " was marked as completed",
                                    "TASK",
                                    saved.getId().toString(),
                                    "/app/tasks"
                            );
                        } catch (Exception e) {
                            log.warn("Failed to create task completed notification: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        if (request.assigneeId() != null && !request.assigneeId().equals(oldAssigneeId)) {
            auditLogRepository.save(new AuditLog(
                    actorId,
                    actorRole != null ? actorRole.name() : null,
                    "TASK_ASSIGNED",
                    "SUCCESS",
                    "task_id=" + saved.getId() + ", project_id=" + saved.getProject().getId() +
                            ", assignee_id=" + saved.getAssigneeId()
            ));

            if (notificationService != null) {
                UUID targetUserId = resolveUserId(request.assigneeId());
                if (targetUserId != null && !targetUserId.equals(actorId)) {
                    try {
                        notificationService.createNotification(
                                targetUserId,
                                com.yourco.saas.domain.collaboration.NotificationType.TASK_ASSIGNED,
                                "Task Assigned: " + saved.getTitle(),
                                "You were assigned to task '" + saved.getTitle() + "' in project " + saved.getProject().getName(),
                                "TASK",
                                saved.getId().toString(),
                                "/app/tasks"
                        );
                    } catch (Exception e) {
                        log.warn("Failed to create task reassigned notification: {}", e.getMessage());
                    }
                }
            }
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "TASK_UPDATED",
                "SUCCESS",
                "task_id=" + saved.getId() + ", project_id=" + saved.getProject().getId()
        ));

        return TaskResponse.fromEntity(saved);
    }

    public void deleteTask(UUID taskId) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        UUID projectId = task.getProject().getId();
        taskRepository.delete(task);

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "TASK_DELETED",
                "SUCCESS",
                "task_id=" + taskId + ", project_id=" + projectId
        ));
    }

    private String getActorEmail(UUID actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId).map(User::getEmail).orElse(null);
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

    private UUID resolveUserId(UUID assigneeOrUserId) {
        if (assigneeOrUserId == null) return null;
        if (userRepository.existsById(assigneeOrUserId)) {
            return assigneeOrUserId;
        }
        ProjectService.ResolvedUser resolved = projectService.resolveUserOrEmployee(assigneeOrUserId);
        if (resolved != null && resolved.email() != null) {
            return userRepository.findByEmail(resolved.email()).map(User::getId).orElse(null);
        }
        return null;
    }
}
