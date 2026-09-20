package com.yourco.saas.collaboration.service;

import com.yourco.saas.collaboration.dto.*;
import com.yourco.saas.collaboration.websocket.WebSocketEventBroadcaster;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.collaboration.CalendarEvent;
import com.yourco.saas.domain.collaboration.CalendarEventAttendee;
import com.yourco.saas.domain.collaboration.CalendarEventAttendeeRepository;
import com.yourco.saas.domain.collaboration.CalendarEventRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@Transactional
public class CalendarService {

    private static final Logger log = LoggerFactory.getLogger(CalendarService.class);

    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventAttendeeRepository attendeeRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final WebSocketEventBroadcaster webSocketEventBroadcaster;
    private final TenantRegistryService tenantRegistryService;

    public CalendarService(CalendarEventRepository calendarEventRepository,
                           CalendarEventAttendeeRepository attendeeRepository,
                           ProjectRepository projectRepository,
                           TaskRepository taskRepository,
                           UserRepository userRepository,
                           EmployeeRepository employeeRepository,
                           AuditLogRepository auditLogRepository,
                           WebSocketEventBroadcaster webSocketEventBroadcaster,
                           TenantRegistryService tenantRegistryService) {
        this.calendarEventRepository = calendarEventRepository;
        this.attendeeRepository = attendeeRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogRepository = auditLogRepository;
        this.webSocketEventBroadcaster = webSocketEventBroadcaster;
        this.tenantRegistryService = tenantRegistryService;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(Instant from, Instant to, UUID projectId) {
        Instant effectiveFrom = from != null ? from : Instant.now().minusSeconds(30L * 24 * 3600);
        Instant effectiveTo = to != null ? to : Instant.now().plusSeconds(60L * 24 * 3600);

        List<CalendarEventResponse> feed = new ArrayList<>();

        // 1. Custom Calendar Events
        List<CalendarEvent> customEvents = (projectId != null)
                ? calendarEventRepository.findByProjectIdInDateRange(projectId, effectiveFrom, effectiveTo)
                : calendarEventRepository.findInDateRange(effectiveFrom, effectiveTo);

        for (CalendarEvent event : customEvents) {
            String projectName = null;
            if (event.getProjectId() != null) {
                projectName = projectRepository.findById(event.getProjectId()).map(Project::getName).orElse(null);
            }

            List<CalendarEventAttendeeDto> attendees = attendeeRepository.findByEventId(event.getId()).stream()
                    .map(att -> {
                        ResolvedUser user = resolveUser(att.getUserId());
                        return new CalendarEventAttendeeDto(att.getUserId(), user.name(), user.email(), att.getStatus());
                    })
                    .toList();

            feed.add(new CalendarEventResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getStartAt(),
                    event.getEndAt(),
                    event.isAllDay(),
                    event.getLocation(),
                    event.getProjectId(),
                    projectName,
                    event.getCreatedBy(),
                    CalendarEventType.CUSTOM,
                    attendees,
                    event.getCreatedAt()
            ));
        }

        // 2. Projected Task Deadlines
        LocalDate fromDate = effectiveFrom.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDate = effectiveTo.atZone(ZoneOffset.UTC).toLocalDate();

        List<Task> tasks;
        if (projectId != null) {
            tasks = taskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        } else {
            tasks = taskRepository.findAll();
        }

        for (Task task : tasks) {
            if (task.getDueDate() != null && !task.getDueDate().isBefore(fromDate) && !task.getDueDate().isAfter(toDate)) {
                Instant dueInstant = task.getDueDate().atStartOfDay().toInstant(ZoneOffset.UTC);
                UUID taskProjId = task.getProject() != null ? task.getProject().getId() : null;
                String taskProjectName = task.getProject() != null ? task.getProject().getName() : "Project";

                List<CalendarEventAttendeeDto> taskAttendees = new ArrayList<>();
                if (task.getAssigneeId() != null) {
                    ResolvedUser assignee = resolveUser(task.getAssigneeId());
                    taskAttendees.add(new CalendarEventAttendeeDto(task.getAssigneeId(), assignee.name(), assignee.email(), "ASSIGNED"));
                }

                feed.add(new CalendarEventResponse(
                        task.getId(),
                        "Task Due: " + task.getTitle(),
                        task.getDescription(),
                        dueInstant,
                        dueInstant.plusSeconds(3600 * 8), // 8-hour span for due day
                        true,
                        taskProjectName,
                        taskProjId,
                        taskProjectName,
                        null,
                        CalendarEventType.TASK_DUE,
                        taskAttendees,
                        task.getCreatedAt()
                ));
            }
        }

        // 3. Projected Project Deadlines
        List<Project> projects = (projectId != null)
                ? projectRepository.findById(projectId).map(List::of).orElse(List.of())
                : projectRepository.findAll();

        for (Project project : projects) {
            if (project.getDueDate() != null && !project.getDueDate().isBefore(fromDate) && !project.getDueDate().isAfter(toDate)) {
                Instant deadlineInstant = project.getDueDate().atStartOfDay().toInstant(ZoneOffset.UTC);
                feed.add(new CalendarEventResponse(
                        project.getId(),
                        "Project Deadline: " + project.getName(),
                        project.getDescription(),
                        deadlineInstant,
                        deadlineInstant.plusSeconds(3600 * 8),
                        true,
                        "Workspace",
                        project.getId(),
                        project.getName(),
                        project.getOwnerId(),
                        CalendarEventType.PROJECT_DEADLINE,
                        List.of(),
                        project.getCreatedAt()
                ));
            }
        }

        feed.sort(Comparator.comparing(CalendarEventResponse::startAt));
        return feed;
    }

    public CalendarEventResponse createEvent(CreateCalendarEventRequest request) {
        if (request.endAt().isBefore(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        String projectName = null;
        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
            projectName = project.getName();
        }

        // Validate all attendees belong to current tenant
        if (request.attendeeIds() != null) {
            for (UUID attendeeId : request.attendeeIds()) {
                if (userRepository.findById(attendeeId).isEmpty() && employeeRepository.findById(attendeeId).isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendee user " + attendeeId + " not found in current tenant");
                }
            }
        }

        UUID actorId = currentActorId();

        CalendarEvent event = new CalendarEvent(
                request.title().trim(),
                request.description(),
                request.startAt(),
                request.endAt(),
                request.allDay(),
                request.location(),
                request.projectId(),
                actorId
        );
        CalendarEvent saved = calendarEventRepository.save(event);

        List<CalendarEventAttendeeDto> attendeeDtos = new ArrayList<>();
        if (request.attendeeIds() != null) {
            for (UUID attendeeId : request.attendeeIds()) {
                CalendarEventAttendee attendee = attendeeRepository.save(new CalendarEventAttendee(saved.getId(), attendeeId, "ACCEPTED"));
                ResolvedUser resolved = resolveUser(attendee.getUserId());
                attendeeDtos.add(new CalendarEventAttendeeDto(attendee.getUserId(), resolved.name(), resolved.email(), attendee.getStatus()));
            }
        }

        auditLogRepository.save(AuditLog.of(
                actorId,
                "CALENDAR_EVENT_CREATED",
                "CalendarEvent",
                saved.getId().toString(),
                "title=" + saved.getTitle()
        ));

        CalendarEventResponse response = new CalendarEventResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStartAt(),
                saved.getEndAt(),
                saved.isAllDay(),
                saved.getLocation(),
                saved.getProjectId(),
                projectName,
                saved.getCreatedBy(),
                CalendarEventType.CUSTOM,
                attendeeDtos,
                saved.getCreatedAt()
        );

        // Broadcast via WebSocket
        String tenantId = currentTenantId();
        if (tenantId != null) {
            try {
                webSocketEventBroadcaster.broadcastCalendarEvent(tenantId, response);
            } catch (Exception e) {
                log.warn("Failed to broadcast WebSocket calendar event: {}", e.getMessage());
            }
        }

        return response;
    }

    public CalendarEventResponse getEventById(UUID id) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        String projectName = null;
        if (event.getProjectId() != null) {
            projectName = projectRepository.findById(event.getProjectId()).map(Project::getName).orElse(null);
        }

        List<CalendarEventAttendeeDto> attendees = attendeeRepository.findByEventId(event.getId()).stream()
                .map(att -> {
                    ResolvedUser user = resolveUser(att.getUserId());
                    return new CalendarEventAttendeeDto(att.getUserId(), user.name(), user.email(), att.getStatus());
                })
                .toList();

        return new CalendarEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartAt(),
                event.getEndAt(),
                event.isAllDay(),
                event.getLocation(),
                event.getProjectId(),
                projectName,
                event.getCreatedBy(),
                CalendarEventType.CUSTOM,
                attendees,
                event.getCreatedAt()
        );
    }

    public CalendarEventResponse updateEvent(UUID id, UpdateCalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        validateCanModify(event);

        if (request.endAt().isBefore(request.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        String projectName = null;
        if (request.projectId() != null) {
            Project project = projectRepository.findById(request.projectId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
            projectName = project.getName();
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setAllDay(request.allDay());
        event.setLocation(request.location());
        event.setProjectId(request.projectId());

        CalendarEvent saved = calendarEventRepository.save(event);

        // Update attendees if provided
        if (request.attendeeIds() != null) {
            attendeeRepository.deleteByEventId(saved.getId());
            for (UUID attendeeId : request.attendeeIds()) {
                if (userRepository.findById(attendeeId).isPresent() || employeeRepository.findById(attendeeId).isPresent()) {
                    attendeeRepository.save(new CalendarEventAttendee(saved.getId(), attendeeId, "ACCEPTED"));
                }
            }
        }

        List<CalendarEventAttendeeDto> attendees = attendeeRepository.findByEventId(saved.getId()).stream()
                .map(att -> {
                    ResolvedUser user = resolveUser(att.getUserId());
                    return new CalendarEventAttendeeDto(att.getUserId(), user.name(), user.email(), att.getStatus());
                })
                .toList();

        auditLogRepository.save(AuditLog.of(
                currentActorId(),
                "CALENDAR_EVENT_UPDATED",
                "CalendarEvent",
                saved.getId().toString(),
                "title=" + saved.getTitle()
        ));

        return new CalendarEventResponse(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getStartAt(),
                saved.getEndAt(),
                saved.isAllDay(),
                saved.getLocation(),
                saved.getProjectId(),
                projectName,
                saved.getCreatedBy(),
                CalendarEventType.CUSTOM,
                attendees,
                saved.getCreatedAt()
        );
    }

    public void deleteEvent(UUID id) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        validateCanModify(event);

        calendarEventRepository.delete(event);

        auditLogRepository.save(AuditLog.of(
                currentActorId(),
                "CALENDAR_EVENT_DELETED",
                "CalendarEvent",
                id.toString(),
                "title=" + event.getTitle()
        ));
    }

    private void validateCanModify(CalendarEvent event) {
        UUID actorId = currentActorId();
        Role role = currentActorRole();
        if (role == Role.SUPER_ADMIN || role == Role.ADMIN || role == Role.MANAGER) {
            return;
        }
        if (actorId != null && actorId.equals(event.getCreatedBy())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: cannot modify this event");
    }

    public record ResolvedUser(String name, String email) {}

    public ResolvedUser resolveUser(UUID id) {
        if (id == null) return new ResolvedUser("System", "");
        Optional<Employee> emp = employeeRepository.findById(id);
        if (emp.isPresent()) {
            return new ResolvedUser(emp.get().getName(), emp.get().getEmail());
        }
        Optional<User> u = userRepository.findById(id);
        if (u.isPresent()) {
            String email = u.get().getEmail();
            String name = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            return new ResolvedUser(name, email);
        }
        return new ResolvedUser("User", "");
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

    private String currentTenantId() {
        String schema = TenantContext.getTenant();
        if (schema == null) return null;
        return tenantRegistryService.findBySchemaName(schema)
                .map(TenantRecord::tenantId)
                .orElse(null);
    }
}
