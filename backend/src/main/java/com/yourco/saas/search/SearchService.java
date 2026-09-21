package com.yourco.saas.search;

import com.yourco.saas.domain.hrm.Department;
import com.yourco.saas.domain.hrm.DepartmentRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveRequestRepository;
import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.search.dto.SearchResultDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public SearchService(EmployeeRepository employeeRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         DepartmentRepository departmentRepository,
                         LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.departmentRepository = departmentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public List<SearchResultDto> search(String query, String typeFilter, Integer limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String q = query.trim().toLowerCase();
        int maxResults = (limit != null && limit > 0 && limit <= 100) ? limit : 25;
        String filter = typeFilter != null ? typeFilter.trim().toUpperCase() : "ALL";

        UUID actorId = currentActorId();
        Role actorRole = currentActorRole();

        List<SearchResultDto> results = new ArrayList<>();

        // 1. Employees
        if ("ALL".equals(filter) || "EMPLOYEE".equals(filter) || "EMPLOYEES".equals(filter)) {
            List<Employee> employees = employeeRepository.searchEmployees(q);
            for (Employee e : employees) {
                results.add(new SearchResultDto(
                        e.getId().toString(),
                        "EMPLOYEE",
                        e.getName(),
                        e.getPosition() + " • " + e.getDepartment(),
                        e.getEmail(),
                        "/app/hrm/employees/" + e.getId(),
                        e.getStatus().name()
                ));
                if (results.size() >= maxResults) return results;
            }
        }

        // 2. Projects
        if ("ALL".equals(filter) || "PROJECT".equals(filter) || "PROJECTS".equals(filter)) {
            List<Project> projects = projectRepository.searchProjects(null, null, null, q);
            for (Project p : projects) {
                results.add(new SearchResultDto(
                        p.getId().toString(),
                        "PROJECT",
                        p.getName(),
                        "Project • " + p.getStatus() + (p.getClient() != null ? " • " + p.getClient() : ""),
                        p.getDescription() != null ? p.getDescription() : "No description",
                        "/app/projects/" + p.getId(),
                        p.getStatus().name()
                ));
                if (results.size() >= maxResults) return results;
            }
        }

        // 3. Tasks
        if ("ALL".equals(filter) || "TASK".equals(filter) || "TASKS".equals(filter)) {
            List<Task> tasks = taskRepository.searchTasks(null, null, null, null, null, null, q);
            for (Task t : tasks) {
                results.add(new SearchResultDto(
                        t.getId().toString(),
                        "TASK",
                        t.getTitle(),
                        "Task • " + t.getStatus() + (t.getProject() != null ? " • " + t.getProject().getName() : ""),
                        t.getDescription() != null ? t.getDescription() : (t.getAssigneeName() != null ? "Assigned to " + t.getAssigneeName() : "Unassigned"),
                        "/app/tasks",
                        t.getStatus().name()
                ));
                if (results.size() >= maxResults) return results;
            }
        }

        // 4. Teams / Departments
        if ("ALL".equals(filter) || "TEAM".equals(filter) || "DEPARTMENT".equals(filter)) {
            List<Department> departments = departmentRepository.findAll();
            for (Department d : departments) {
                if (d.getName().toLowerCase().contains(q) || (d.getLead() != null && d.getLead().toLowerCase().contains(q))) {
                    results.add(new SearchResultDto(
                            d.getId().toString(),
                            "TEAM",
                            d.getName(),
                            "Department" + (d.getLead() != null ? " • Lead: " + d.getLead() : ""),
                            "Budget utilization: " + d.getBudgetUtilization() + "%",
                            "/app/hrm/teams",
                            "ACTIVE"
                    ));
                    if (results.size() >= maxResults) return results;
                }
            }
        }

        // 5. Leave Requests (RBAC-aware: Regular USER only sees their own)
        if ("ALL".equals(filter) || "LEAVE".equals(filter)) {
            UUID searchUserId = (actorRole == Role.USER) ? actorId : null;
            List<LeaveRequest> requests = leaveRequestRepository.searchRequests(searchUserId, null, null, null, null, q);
            for (LeaveRequest r : requests) {
                results.add(new SearchResultDto(
                        r.getId().toString(),
                        "LEAVE",
                        r.getEmployeeName() + " — " + r.getLeaveType() + " (" + r.getDaysCount() + "d)",
                        "Leave • " + r.getStatus() + " • " + r.getStartDate() + " to " + r.getEndDate(),
                        r.getReason(),
                        "/app/leave",
                        r.getStatus().name()
                ));
                if (results.size() >= maxResults) return results;
            }
        }

        return results;
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
