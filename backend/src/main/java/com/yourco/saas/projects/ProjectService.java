package com.yourco.saas.projects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.projects.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.projects.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.yourco.saas.collaboration.service.NotificationService notificationService;

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          ProjectMemberRepository projectMemberRepository,
                          EmployeeRepository employeeRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjects(ProjectStatus status, ProjectPriority priority, UUID ownerId, String search) {
        List<Project> projects = projectRepository.searchProjects(status, priority, ownerId, search);
        return projects.stream().map(this::toProjectResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(UUID id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return toProjectResponse(project);
    }

    public ProjectResponse createProject(CreateProjectRequest request) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        String ownerName = null;
        UUID ownerId = request.ownerId();
        if (ownerId != null) {
            ResolvedUser resolved = resolveUserOrEmployee(ownerId);
            if (resolved == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project owner must belong to the current workspace");
            }
            ownerName = resolved.name();
        } else if (actorId != null) {
            ownerId = actorId;
            ownerName = currentActorName();
        }

        Project project = new Project(
                request.name().trim(),
                request.description(),
                request.client(),
                request.status() != null ? request.status() : ProjectStatus.PLANNING,
                request.priority() != null ? request.priority() : ProjectPriority.MEDIUM,
                request.startDate(),
                request.dueDate(),
                request.budget(),
                ownerId,
                ownerName
        );

        Project saved = projectRepository.save(project);

        if (request.memberIds() != null && !request.memberIds().isEmpty()) {
            for (UUID mId : request.memberIds()) {
                ResolvedUser res = resolveUserOrEmployee(mId);
                if (res == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team member " + mId + " must belong to the current workspace");
                }
                ProjectMember pm = new ProjectMember(saved, mId, res.name(), res.email(), "MEMBER");
                projectMemberRepository.save(pm);
            }
        }

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "PROJECT_CREATED",
                "SUCCESS",
                "project_id=" + saved.getId() + ", name=" + saved.getName()
        ));

        return toProjectResponse(saved);
    }

    public ProjectResponse updateProject(UUID id, UpdateProjectRequest request) {
        validateManagerOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (request.name() != null && !request.name().isBlank()) {
            project.setName(request.name().trim());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.client() != null) {
            project.setClient(request.client());
        }
        if (request.priority() != null) {
            project.setPriority(request.priority());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.dueDate() != null) {
            project.setDueDate(request.dueDate());
        }
        if (request.budget() != null) {
            project.setBudget(request.budget());
        }

        boolean wasArchived = false;
        if (request.status() != null) {
            if (request.status() == ProjectStatus.ARCHIVED && project.getStatus() != ProjectStatus.ARCHIVED) {
                wasArchived = true;
            }
            project.setStatus(request.status());
        }

        if (request.ownerId() != null) {
            ResolvedUser resolved = resolveUserOrEmployee(request.ownerId());
            if (resolved == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project owner must belong to the current workspace");
            }
            project.setOwnerId(request.ownerId());
            project.setOwnerName(resolved.name());
        }

        if (request.memberIds() != null) {
            List<ProjectMember> existing = projectMemberRepository.findByProjectId(id);
            Set<UUID> existingIds = new HashSet<>();
            for (ProjectMember em : existing) {
                if (!request.memberIds().contains(em.getMemberId())) {
                    projectMemberRepository.delete(em);
                } else {
                    existingIds.add(em.getMemberId());
                }
            }
            for (UUID mId : request.memberIds()) {
                if (!existingIds.contains(mId)) {
                    ResolvedUser res = resolveUserOrEmployee(mId);
                    if (res == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team member " + mId + " must belong to the current workspace");
                    }
                    ProjectMember pm = new ProjectMember(project, mId, res.name(), res.email(), "MEMBER");
                    projectMemberRepository.save(pm);
                }
            }
        }

        Project saved = projectRepository.save(project);

        String action = wasArchived ? "PROJECT_ARCHIVED" : "PROJECT_UPDATED";
        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                action,
                "SUCCESS",
                "project_id=" + saved.getId() + ", name=" + saved.getName()
        ));

        return toProjectResponse(saved);
    }

    public void deleteProject(UUID id) {
        validateAdminOrAbove();

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        String projectName = project.getName();
        projectRepository.delete(project);

        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "PROJECT_DELETED",
                "SUCCESS",
                "project_id=" + id + ", name=" + projectName
        ));
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberDto> getProjectMembers(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(ProjectMemberDto::fromEntity)
                .toList();
    }

    public ProjectMemberDto addProjectMember(UUID projectId, AddProjectMemberRequest request) {
        validateManagerOrAbove();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        ResolvedUser resolved = resolveUserOrEmployee(request.memberId());
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member must belong to the current workspace");
        }

        if (projectMemberRepository.existsByProjectIdAndMemberId(projectId, request.memberId())) {
            ProjectMember existing = projectMemberRepository.findByProjectIdAndMemberId(projectId, request.memberId()).orElseThrow();
            return ProjectMemberDto.fromEntity(existing);
        }

        ProjectMember member = new ProjectMember(project, request.memberId(), resolved.name(), resolved.email(), request.role());
        ProjectMember saved = projectMemberRepository.save(member);

        auditLogRepository.save(new AuditLog(
                currentActorId(),
                currentActorRole() != null ? currentActorRole().name() : null,
                "PROJECT_UPDATED",
                "SUCCESS",
                "project_id=" + projectId + ", action=ADD_MEMBER, member_id=" + request.memberId()
        ));

        if (notificationService != null) {
            UUID targetUserId = resolveUserId(request.memberId());
            if (targetUserId != null && !targetUserId.equals(currentActorId())) {
                try {
                    notificationService.createNotification(
                            targetUserId,
                            com.yourco.saas.domain.collaboration.NotificationType.PROJECT_MEMBER_ADDED,
                            "Added to Project: " + project.getName(),
                            "You were added as a team member to project '" + project.getName() + "'",
                            "PROJECT",
                            projectId.toString(),
                            "/app/projects/" + projectId
                    );
                } catch (Exception e) {
                    log.warn("Failed to create member notification: {}", e.getMessage());
                }
            }
        }

        return ProjectMemberDto.fromEntity(saved);
    }

    public void removeProjectMember(UUID projectId, UUID memberId) {
        validateManagerOrAbove();

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        projectMemberRepository.deleteByProjectIdAndMemberId(projectId, memberId);

        auditLogRepository.save(new AuditLog(
                currentActorId(),
                currentActorRole() != null ? currentActorRole().name() : null,
                "PROJECT_UPDATED",
                "SUCCESS",
                "project_id=" + projectId + ", action=REMOVE_MEMBER, member_id=" + memberId
        ));
    }

    @Transactional(readOnly = true)
    public ProjectStatsResponse getStats() {
        UUID actorId = currentActorId();
        long activeProjects = projectRepository.countByStatus(ProjectStatus.ACTIVE);
        long totalProjects = projectRepository.count();
        long openTasks = taskRepository.countByStatusNot(TaskStatus.DONE);
        long overdueTasks = taskRepository.countByDueDateBeforeAndStatusNot(LocalDate.now(), TaskStatus.DONE);
        long completedTasks = taskRepository.countByStatus(TaskStatus.DONE);
        long myTasks = actorId != null ? taskRepository.countByAssigneeIdAndStatusNot(actorId, TaskStatus.DONE) : 0;

        return new ProjectStatsResponse(activeProjects, totalProjects, openTasks, overdueTasks, completedTasks, myTasks);
    }

    @Transactional(readOnly = true)
    public List<ProjectActivityDto> getRecentActivities(UUID projectId) {
        List<AuditLog> logs;
        if (projectId != null) {
            logs = auditLogRepository.findProjectActivities(projectId.toString(), PageRequest.of(0, 25));
        } else {
            logs = auditLogRepository.findRecentProjectAndTaskActivities(PageRequest.of(0, 25));
        }

        return logs.stream().map(this::toActivityDto).toList();
    }

    private ProjectActivityDto toActivityDto(AuditLog log) {
        String actor = log.getActorRole() != null ? log.getActorRole() : "System";
        if (log.getActorId() != null) {
            ResolvedUser res = resolveUserOrEmployee(log.getActorId());
            if (res != null) {
                actor = res.name();
            }
        }

        String action = humanizeAction(log.getAction());
        String target = extractTarget(log.getDetails(), log.getAction());
        String type = "info";
        if ("FAILURE".equals(log.getOutcome())) {
            type = "warning";
        } else if (log.getAction().contains("COMPLETED") || log.getAction().contains("DONE")) {
            type = "success";
        }

        String timestamp = formatRelativeTime(log.getCreatedAt());

        return new ProjectActivityDto(
                log.getId().toString(),
                actor,
                action,
                target,
                timestamp,
                type,
                log.getCreatedAt()
        );
    }

    private String humanizeAction(String action) {
        if (action == null) return "updated";
        return switch (action) {
            case "PROJECT_CREATED" -> "created project";
            case "PROJECT_UPDATED" -> "updated project";
            case "PROJECT_ARCHIVED" -> "archived project";
            case "PROJECT_DELETED" -> "deleted project";
            case "TASK_CREATED" -> "created task";
            case "TASK_UPDATED" -> "updated task";
            case "TASK_STATUS_CHANGED" -> "updated status for";
            case "TASK_ASSIGNED" -> "assigned task";
            case "TASK_COMPLETED" -> "completed task";
            case "TASK_DELETED" -> "deleted task";
            default -> action.toLowerCase().replace('_', ' ');
        };
    }

    private String extractTarget(String details, String action) {
        if (details == null) return "workspace item";
        if (details.contains("title=")) {
            String sub = details.substring(details.indexOf("title=") + 6);
            int comma = sub.indexOf(',');
            return comma > 0 ? sub.substring(0, comma) : sub;
        }
        if (details.contains("name=")) {
            String sub = details.substring(details.indexOf("name=") + 5);
            int comma = sub.indexOf(',');
            return comma > 0 ? sub.substring(0, comma) : sub;
        }
        return "project item";
    }

    private String formatRelativeTime(Instant instant) {
        if (instant == null) return "recently";
        Duration duration = Duration.between(instant, Instant.now());
        long seconds = duration.getSeconds();
        if (seconds < 60) return "just now";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        long days = hours / 24;
        return days + " day" + (days > 1 ? "s" : "") + " ago";
    }

    public ProjectResponse toProjectResponse(Project project) {
        int totalTasks = (int) taskRepository.countByProjectId(project.getId());
        int doneTasks = (int) taskRepository.countByProjectIdAndStatus(project.getId(), TaskStatus.DONE);
        int overdueTasks = (int) taskRepository.countByProjectIdAndDueDateBeforeAndStatusNot(
                project.getId(), LocalDate.now(), TaskStatus.DONE);
        List<ProjectMemberDto> members = projectMemberRepository.findByProjectId(project.getId()).stream()
                .map(ProjectMemberDto::fromEntity)
                .toList();

        return ProjectResponse.fromEntity(project, totalTasks, doneTasks, overdueTasks, members);
    }

    public record ResolvedUser(String name, String email) {}

    public ResolvedUser resolveUserOrEmployee(UUID id) {
        if (id == null) return null;
        Optional<Employee> emp = employeeRepository.findById(id);
        if (emp.isPresent()) {
            return new ResolvedUser(emp.get().getName(), emp.get().getEmail());
        }
        Optional<User> u = userRepository.findById(id);
        if (u.isPresent()) {
            return new ResolvedUser(u.get().getEmail(), u.get().getEmail());
        }
        return null;
    }

    public UUID resolveUserId(UUID memberOrUserId) {
        if (memberOrUserId == null) return null;
        if (userRepository.existsById(memberOrUserId)) {
            return memberOrUserId;
        }
        return employeeRepository.findById(memberOrUserId)
                .map(Employee::getEmail)
                .flatMap(userRepository::findByEmail)
                .map(User::getId)
                .orElse(null);
    }

    private void validateManagerOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN && role != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions: Requires MANAGER or above");
        }
    }

    private void validateAdminOrAbove() {
        Role role = currentActorRole();
        if (role != Role.SUPER_ADMIN && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions: Requires ADMIN or above");
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
        ResolvedUser resolved = resolveUserOrEmployee(actorId);
        return resolved != null ? resolved.name() : "User";
    }
}
