package com.yourco.saas.reports;

import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.hrm.Department;
import com.yourco.saas.domain.hrm.DepartmentRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveRequestRepository;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.reports.dto.LeaveReportDto;
import com.yourco.saas.reports.dto.ProjectsReportDto;
import com.yourco.saas.reports.dto.TasksReportDto;
import com.yourco.saas.reports.dto.WorkforceReportDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportService(EmployeeRepository employeeRepository,
                         DepartmentRepository departmentRepository,
                         UserRepository userRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         AuditLogRepository auditLogRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public WorkforceReportDto getWorkforceReport(String department, String status) {
        List<Employee> all = employeeRepository.findAll();
        List<User> users = userRepository.findAll();

        List<Employee> filtered = all.stream()
                .filter(e -> department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)
                        || e.getDepartment().equalsIgnoreCase(department.trim()))
                .filter(e -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)
                        || e.getStatus().name().equalsIgnoreCase(status.trim()))
                .toList();

        long total = all.size();
        long active = all.stream().filter(e -> e.getStatus() == EmployeeStatus.ACTIVE).count();
        long onLeave = all.stream().filter(e -> e.getStatus() == EmployeeStatus.ON_LEAVE).count();
        long probation = all.stream().filter(e -> e.getStatus() == EmployeeStatus.PROBATION).count();
        long inactive = all.stream().filter(e -> e.getStatus() == EmployeeStatus.INACTIVE || e.getStatus() == EmployeeStatus.TERMINATED).count();

        Map<String, Long> roleDist = users.stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        Map<String, Long> deptDist = all.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        List<WorkforceReportDto.EmployeeReportItemDto> items = filtered.stream()
                .map(e -> new WorkforceReportDto.EmployeeReportItemDto(
                        e.getId(),
                        e.getEmployeeId(),
                        e.getName(),
                        e.getEmail(),
                        e.getDepartment(),
                        e.getPosition(),
                        e.getStatus().name(),
                        e.getHireDate(),
                        e.getAttendanceRate(),
                        e.getBillableHours()
                ))
                .toList();

        return new WorkforceReportDto(total, active, inactive, onLeave, probation, roleDist, deptDist, items);
    }

    public ProjectsReportDto getProjectsReport(ProjectStatus status, ProjectPriority priority) {
        List<Project> all = projectRepository.findAllByOrderByCreatedAtDesc();
        List<Task> tasks = taskRepository.findAll();

        Map<UUID, List<Task>> tasksByProject = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        long total = all.size();
        long active = all.stream().filter(p -> p.getStatus() == ProjectStatus.ACTIVE).count();
        long completed = all.stream().filter(p -> p.getStatus() == ProjectStatus.COMPLETED).count();
        long planning = all.stream().filter(p -> p.getStatus() == ProjectStatus.PLANNING).count();
        long onHold = all.stream().filter(p -> p.getStatus() == ProjectStatus.ON_HOLD).count();
        long archived = all.stream().filter(p -> p.getStatus() == ProjectStatus.ARCHIVED).count();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));

        Map<String, Long> byPriority = all.stream()
                .collect(Collectors.groupingBy(p -> p.getPriority().name(), Collectors.counting()));

        List<ProjectsReportDto.ProjectReportItemDto> items = all.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> priority == null || p.getPriority() == priority)
                .map(p -> {
                    List<Task> pTasks = tasksByProject.getOrDefault(p.getId(), List.of());
                    int tot = pTasks.size();
                    int done = (int) pTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
                    double prog = tot > 0 ?
                            BigDecimal.valueOf((done * 100.0) / tot).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;
                    return new ProjectsReportDto.ProjectReportItemDto(
                            p.getId(),
                            p.getName(),
                            p.getClient(),
                            p.getStatus().name(),
                            p.getPriority().name(),
                            p.getStartDate(),
                            p.getDueDate(),
                            p.getBudget(),
                            tot,
                            done,
                            prog
                    );
                })
                .toList();

        return new ProjectsReportDto(total, active, completed, planning, onHold, archived, byStatus, byPriority, items);
    }

    public TasksReportDto getTasksReport(UUID projectId, TaskStatus status, TaskPriority priority) {
        List<Task> all = taskRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();

        long total = all.size();
        long completed = all.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long open = total - completed;
        long overdue = all.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.DONE).count();

        double completionRate = total > 0 ?
                BigDecimal.valueOf((completed * 100.0) / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));

        Map<String, Long> byPriority = all.stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));

        List<TasksReportDto.TaskReportItemDto> items = all.stream()
                .filter(t -> projectId == null || t.getProject().getId().equals(projectId))
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .map(t -> new TasksReportDto.TaskReportItemDto(
                        t.getId(),
                        t.getTitle(),
                        t.getProject().getName(),
                        t.getStatus().name(),
                        t.getPriority().name(),
                        t.getAssigneeName(),
                        t.getDueDate(),
                        t.getEstimatedHours(),
                        t.getActualHours()
                ))
                .toList();

        return new TasksReportDto(total, open, completed, overdue, completionRate, byStatus, byPriority, items);
    }

    public LeaveReportDto getLeaveReport(LeaveStatus status, LeaveType leaveType, LocalDate fromDate, LocalDate toDate) {
        List<LeaveRequest> all = leaveRequestRepository.searchRequests(null, status, leaveType, fromDate, toDate, null);

        long total = all.size();
        long pending = all.stream().filter(r -> r.getStatus() == LeaveStatus.PENDING).count();
        long approved = all.stream().filter(r -> r.getStatus() == LeaveStatus.APPROVED).count();
        long rejected = all.stream().filter(r -> r.getStatus() == LeaveStatus.REJECTED).count();
        long cancelled = all.stream().filter(r -> r.getStatus() == LeaveStatus.CANCELLED).count();

        BigDecimal totalDaysApproved = all.stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED)
                .map(LeaveRequest::getDaysCount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        Map<String, Long> byType = all.stream()
                .collect(Collectors.groupingBy(r -> r.getLeaveType().name(), Collectors.counting()));

        List<LeaveReportDto.LeaveReportItemDto> items = all.stream()
                .map(r -> new LeaveReportDto.LeaveReportItemDto(
                        r.getId(),
                        r.getEmployeeName(),
                        r.getEmployeeEmail(),
                        r.getLeaveType().name(),
                        r.getStartDate(),
                        r.getEndDate(),
                        r.getDaysCount(),
                        r.getStatus().name(),
                        r.getReviewerName(),
                        r.getReviewedAt()
                ))
                .toList();

        return new LeaveReportDto(total, pending, approved, rejected, cancelled, totalDaysApproved, byStatus, byType, items);
    }

    @Transactional
    public String exportWorkforceCsv() {
        auditExport("WORKFORCE");
        WorkforceReportDto report = getWorkforceReport(null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Employee ID,Name,Email,Department,Position,Status,Hire Date,Attendance Rate,Billable Hours\n");
        for (WorkforceReportDto.EmployeeReportItemDto emp : report.employees()) {
            sb.append(escapeCsv(emp.employeeId())).append(",")
              .append(escapeCsv(emp.name())).append(",")
              .append(escapeCsv(emp.email())).append(",")
              .append(escapeCsv(emp.department())).append(",")
              .append(escapeCsv(emp.position())).append(",")
              .append(escapeCsv(emp.status())).append(",")
              .append(emp.hireDate()).append(",")
              .append(emp.attendanceRate()).append(",")
              .append(emp.billableHours()).append("\n");
        }
        return sb.toString();
    }

    @Transactional
    public String exportProjectsCsv() {
        auditExport("PROJECTS");
        ProjectsReportDto report = getProjectsReport(null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Project Name,Client,Status,Priority,Start Date,Due Date,Budget,Total Tasks,Completed Tasks,Progress %\n");
        for (ProjectsReportDto.ProjectReportItemDto p : report.projects()) {
            sb.append(escapeCsv(p.name())).append(",")
              .append(escapeCsv(p.client() != null ? p.client() : "")).append(",")
              .append(escapeCsv(p.status())).append(",")
              .append(escapeCsv(p.priority())).append(",")
              .append(p.startDate() != null ? p.startDate() : "").append(",")
              .append(p.dueDate() != null ? p.dueDate() : "").append(",")
              .append(p.budget() != null ? p.budget() : BigDecimal.ZERO).append(",")
              .append(p.totalTasks()).append(",")
              .append(p.doneTasks()).append(",")
              .append(p.progressPercentage()).append("\n");
        }
        return sb.toString();
    }

    @Transactional
    public String exportTasksCsv() {
        auditExport("TASKS");
        TasksReportDto report = getTasksReport(null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Task Title,Project Name,Status,Priority,Assignee,Due Date,Estimated Hours,Actual Hours\n");
        for (TasksReportDto.TaskReportItemDto t : report.tasks()) {
            sb.append(escapeCsv(t.title())).append(",")
              .append(escapeCsv(t.projectName())).append(",")
              .append(escapeCsv(t.status())).append(",")
              .append(escapeCsv(t.priority())).append(",")
              .append(escapeCsv(t.assigneeName() != null ? t.assigneeName() : "Unassigned")).append(",")
              .append(t.dueDate() != null ? t.dueDate() : "").append(",")
              .append(t.estimatedHours() != null ? t.estimatedHours() : 0).append(",")
              .append(t.actualHours() != null ? t.actualHours() : 0).append("\n");
        }
        return sb.toString();
    }

    @Transactional
    public String exportLeaveCsv() {
        auditExport("LEAVE");
        LeaveReportDto report = getLeaveReport(null, null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Employee Name,Email,Leave Type,Start Date,End Date,Days,Status,Reviewer\n");
        for (LeaveReportDto.LeaveReportItemDto r : report.requests()) {
            sb.append(escapeCsv(r.employeeName())).append(",")
              .append(escapeCsv(r.employeeEmail())).append(",")
              .append(escapeCsv(r.leaveType())).append(",")
              .append(r.startDate()).append(",")
              .append(r.endDate()).append(",")
              .append(r.daysCount()).append(",")
              .append(escapeCsv(r.status())).append(",")
              .append(escapeCsv(r.reviewerName() != null ? r.reviewerName() : "")).append("\n");
        }
        return sb.toString();
    }

    private void auditExport(String reportType) {
        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();
        auditLogRepository.save(new AuditLog(
                actorId,
                actorRole != null ? actorRole.name() : null,
                "REPORT_EXPORTED",
                "SUCCESS",
                "report_type=" + reportType + ",format=CSV"
        ));
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
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
