package com.yourco.saas.selfservice;

import com.yourco.saas.collaboration.dto.CalendarEventResponse;
import com.yourco.saas.collaboration.dto.NotificationResponse;
import com.yourco.saas.collaboration.service.CalendarService;
import com.yourco.saas.collaboration.service.NotificationService;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.projects.ProjectMemberRepository;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.leave.LeaveService;
import com.yourco.saas.leave.dto.LeaveBalanceResponse;
import com.yourco.saas.leave.dto.LeaveRequestResponse;
import com.yourco.saas.projects.ProjectService;
import com.yourco.saas.projects.dto.ProjectResponse;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.selfservice.dto.SelfServiceOverviewDto;
import com.yourco.saas.selfservice.dto.SelfServiceProfileDto;
import com.yourco.saas.selfservice.dto.UpdateSelfServiceProfileRequest;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class SelfServiceService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectService projectService;
    private final LeaveService leaveService;
    private final AuditLogRepository auditLogRepository;
    private final TenantRegistryService tenantRegistryService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CalendarService calendarService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private NotificationService notificationService;

    public SelfServiceService(UserRepository userRepository,
                              EmployeeRepository employeeRepository,
                              TaskRepository taskRepository,
                              ProjectRepository projectRepository,
                              ProjectMemberRepository projectMemberRepository,
                              ProjectService projectService,
                              LeaveService leaveService,
                              AuditLogRepository auditLogRepository,
                              TenantRegistryService tenantRegistryService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectService = projectService;
        this.leaveService = leaveService;
        this.auditLogRepository = auditLogRepository;
        this.tenantRegistryService = tenantRegistryService;
    }

    @Transactional(readOnly = true)
    public SelfServiceProfileDto getProfile() {
        User user = getCurrentUser();
        Employee emp = employeeRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        String tenantId = resolveCurrentTenantId();
        return SelfServiceProfileDto.from(user, emp, tenantId);
    }

    public SelfServiceProfileDto updateProfile(UpdateSelfServiceProfileRequest req) {
        User user = getCurrentUser();

        // STRICT SECURITY: Never allow modifying protected fields
        if (req.role() != null && !req.role().isBlank() && !req.role().equalsIgnoreCase(user.getRole().name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot modify their own role");
        }
        if (req.status() != null && !req.status().isBlank() && !req.status().equalsIgnoreCase(user.getStatus().name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot modify their own account status");
        }
        if (req.email() != null && !req.email().isBlank() && !req.email().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot modify their primary email address");
        }
        if (req.tenantId() != null && !req.tenantId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Users cannot modify tenant identity");
        }

        Employee emp = employeeRepository.findByEmailIgnoreCase(user.getEmail()).orElse(null);
        if (emp == null) {
            // Create employee profile linked to user email
            emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            emp.setEmail(user.getEmail());
            emp.setName(req.name() != null && !req.name().isBlank() ? req.name().trim() : user.getEmail().split("@")[0]);
            emp.setDepartment("General");
            emp.setPosition("Team Member");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setWorkModel(req.workModel() != null ? req.workModel() : "Hybrid");
            emp.setPhone(req.phone());
            emp.setLocation(req.location());
        } else {
            if (req.name() != null && !req.name().isBlank()) {
                emp.setName(req.name().trim());
            }
            if (req.phone() != null) {
                emp.setPhone(req.phone().trim());
            }
            if (req.location() != null) {
                emp.setLocation(req.location().trim());
            }
            if (req.workModel() != null && !req.workModel().isBlank()) {
                emp.setWorkModel(req.workModel().trim());
            }
        }

        Employee saved = employeeRepository.save(emp);

        auditLogRepository.save(new AuditLog(
                user.getId(),
                user.getRole().name(),
                "PROFILE_UPDATED",
                "SUCCESS",
                "user_id=" + user.getId() + ", name=" + saved.getName()
        ));

        String tenantId = resolveCurrentTenantId();
        return SelfServiceProfileDto.from(user, saved, tenantId);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAssignedTasks() {
        User user = getCurrentUser();
        Optional<Employee> emp = employeeRepository.findByEmailIgnoreCase(user.getEmail());

        List<Task> allTasks = taskRepository.findAllByOrderByCreatedAtDesc();
        return allTasks.stream()
                .filter(t -> {
                    if (t.getAssigneeId() != null && t.getAssigneeId().equals(user.getId())) return true;
                    if (emp.isPresent() && t.getAssigneeId() != null && t.getAssigneeId().equals(emp.get().getId())) return true;
                    return t.getAssigneeEmail() != null && t.getAssigneeEmail().equalsIgnoreCase(user.getEmail());
                })
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAssignedProjects() {
        User user = getCurrentUser();
        Optional<Employee> emp = employeeRepository.findByEmailIgnoreCase(user.getEmail());

        Set<UUID> projectIds = new HashSet<>();

        // Projects owned by user
        projectRepository.findByOwnerId(user.getId()).forEach(p -> projectIds.add(p.getId()));

        // Projects where user or emp is a member
        projectMemberRepository.findByMemberId(user.getId()).forEach(pm -> projectIds.add(pm.getProject().getId()));
        emp.ifPresent(e -> projectMemberRepository.findByMemberId(e.getId()).forEach(pm -> projectIds.add(pm.getProject().getId())));

        return projectIds.stream()
                .map(projectRepository::findById)
                .flatMap(Optional::stream)
                .map(projectService::toProjectResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SelfServiceOverviewDto getOverview() {
        SelfServiceProfileDto profile = getProfile();
        List<TaskResponse> tasks = getAssignedTasks();
        List<ProjectResponse> projects = getAssignedProjects();
        List<LeaveBalanceResponse> leaveBalances = leaveService.getMyLeaveBalances();
        List<LeaveRequestResponse> leaveRequests = leaveService.getMyLeaveRequests();

        List<NotificationResponse> notifications = List.of();
        if (notificationService != null) {
            try {
                notifications = notificationService.getNotifications(PageRequest.of(0, 10)).getContent();
            } catch (Exception ignored) {}
        }

        List<CalendarEventResponse> upcomingEvents = getUpcomingEvents();

        return new SelfServiceOverviewDto(
                profile,
                tasks,
                projects,
                leaveBalances,
                leaveRequests,
                notifications,
                upcomingEvents
        );
    }

    private List<CalendarEventResponse> getUpcomingEvents() {
        if (calendarService == null) {
            return List.of();
        }

        User user = getCurrentUser();
        Instant now = Instant.now();
        Instant in30Days = now.plus(30, ChronoUnit.DAYS);

        try {
            List<CalendarEventResponse> events = calendarService.getEvents(now, in30Days, null);
            return events.stream()
                    .filter(e -> (e.createdBy() != null && e.createdBy().equals(user.getId()))
                            || (e.attendees() != null && e.attendees().stream().anyMatch(a -> a.userId().equals(user.getId()))))
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private User getCurrentUser() {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
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

    private String resolveCurrentTenantId() {
        String schema = TenantContext.getTenant();
        if (schema != null) {
            Optional<TenantRecord> record = tenantRegistryService.findBySchemaName(schema);
            if (record.isPresent()) {
                return record.get().tenantId();
            }
        }
        return "unknown";
    }
}
