package com.yourco.saas.workload;

import com.yourco.saas.domain.hrm.Department;
import com.yourco.saas.domain.hrm.DepartmentRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.projects.Project;
import com.yourco.saas.domain.projects.ProjectRepository;
import com.yourco.saas.domain.projects.Task;
import com.yourco.saas.domain.projects.TaskRepository;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.workload.dto.DepartmentWorkloadDto;
import com.yourco.saas.workload.dto.EmployeeWorkloadDto;
import com.yourco.saas.workload.dto.ProjectWorkloadDto;
import com.yourco.saas.workload.dto.WorkforceWorkloadSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class WorkloadService {

    public static final int STANDARD_WEEKLY_CAPACITY_HOURS = 40;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public WorkloadService(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    public WorkforceWorkloadSummaryDto getWorkforceSummary() {
        List<Employee> employees = employeeRepository.findAll();
        List<Task> allTasks = taskRepository.findAllByOrderByCreatedAtDesc();
        List<Project> allProjects = projectRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();
        LocalDate dueSoonThreshold = today.plusDays(7);

        int totalTasks = allTasks.size();
        int openTasks = 0;
        int completedTasks = 0;
        int overdueTasks = 0;
        int dueSoonTasks = 0;
        int totalEstimatedHours = 0;
        int totalActualHours = 0;

        for (Task t : allTasks) {
            boolean isDone = t.getStatus() == TaskStatus.DONE;
            if (isDone) {
                completedTasks++;
            } else {
                openTasks++;
            }

            if (t.getDueDate() != null && !isDone) {
                if (t.getDueDate().isBefore(today)) {
                    overdueTasks++;
                } else if (!t.getDueDate().isAfter(dueSoonThreshold)) {
                    dueSoonTasks++;
                }
            }

            if (t.getEstimatedHours() != null) {
                totalEstimatedHours += t.getEstimatedHours();
            }
            if (t.getActualHours() != null) {
                totalActualHours += t.getActualHours();
            }
        }

        List<EmployeeWorkloadDto> employeeWorkloads = employees.stream()
                .map(emp -> computeEmployeeWorkload(emp, allTasks, today, dueSoonThreshold))
                .sorted(Comparator.comparingInt(EmployeeWorkloadDto::openTasks).reversed())
                .toList();

        List<DepartmentWorkloadDto> departmentWorkloads = computeDepartmentWorkloads(employees, employeeWorkloads);
        List<ProjectWorkloadDto> projectWorkloads = computeProjectWorkloads(allProjects, allTasks, today);

        double avgUtilization = 0.0;
        if (!employeeWorkloads.isEmpty()) {
            double sumUtil = employeeWorkloads.stream().mapToDouble(EmployeeWorkloadDto::capacityUtilization).sum();
            avgUtilization = BigDecimal.valueOf(sumUtil / employeeWorkloads.size())
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
        }

        return new WorkforceWorkloadSummaryDto(
                employees.size(),
                totalTasks,
                openTasks,
                completedTasks,
                overdueTasks,
                dueSoonTasks,
                totalEstimatedHours,
                totalActualHours,
                STANDARD_WEEKLY_CAPACITY_HOURS,
                avgUtilization,
                employeeWorkloads,
                departmentWorkloads,
                projectWorkloads
        );
    }

    public List<EmployeeWorkloadDto> getEmployeeWorkloads() {
        List<Employee> employees = employeeRepository.findAll();
        List<Task> allTasks = taskRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();
        LocalDate dueSoonThreshold = today.plusDays(7);

        return employees.stream()
                .map(emp -> computeEmployeeWorkload(emp, allTasks, today, dueSoonThreshold))
                .sorted(Comparator.comparingInt(EmployeeWorkloadDto::openTasks).reversed())
                .toList();
    }

    public List<ProjectWorkloadDto> getProjectWorkloads() {
        List<Project> allProjects = projectRepository.findAllByOrderByCreatedAtDesc();
        List<Task> allTasks = taskRepository.findAllByOrderByCreatedAtDesc();
        return computeProjectWorkloads(allProjects, allTasks, LocalDate.now());
    }

    public EmployeeWorkloadDto getMyWorkload() {
        UUID actorId = currentActorId();
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Optional<Employee> empOpt = employeeRepository.findByEmailIgnoreCase(user.getEmail());
        Employee employee;
        if (empOpt.isPresent()) {
            employee = empOpt.get();
        } else {
            employee = new Employee();
            employee.setName(user.getEmail().split("@")[0]);
            employee.setEmail(user.getEmail());
            employee.setDepartment("General");
        }

        List<Task> allTasks = taskRepository.findAllByOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();
        LocalDate dueSoonThreshold = today.plusDays(7);

        return computeEmployeeWorkload(employee, allTasks, today, dueSoonThreshold);
    }

    private EmployeeWorkloadDto computeEmployeeWorkload(Employee emp, List<Task> allTasks,
                                                        LocalDate today, LocalDate dueSoonThreshold) {
        int total = 0;
        int open = 0;
        int completed = 0;
        int overdue = 0;
        int dueSoon = 0;
        int estimatedTotal = 0;
        int estimatedRemaining = 0;
        int actual = 0;

        for (Task t : allTasks) {
            boolean matches = (emp.getId() != null && emp.getId().equals(t.getAssigneeId()))
                    || (t.getAssigneeEmail() != null && t.getAssigneeEmail().equalsIgnoreCase(emp.getEmail()));

            if (!matches) continue;

            total++;
            boolean isDone = t.getStatus() == TaskStatus.DONE;
            int est = t.getEstimatedHours() != null ? t.getEstimatedHours() : 0;
            int act = t.getActualHours() != null ? t.getActualHours() : 0;

            estimatedTotal += est;
            actual += act;

            if (isDone) {
                completed++;
            } else {
                open++;
                estimatedRemaining += est;

                if (t.getDueDate() != null) {
                    if (t.getDueDate().isBefore(today)) {
                        overdue++;
                    } else if (!t.getDueDate().isAfter(dueSoonThreshold)) {
                        dueSoon++;
                    }
                }
            }
        }

        // Deterministic capacity calculation: (estimatedRemaining / 40h) * 100
        double rawUtilization = (estimatedRemaining / (double) STANDARD_WEEKLY_CAPACITY_HOURS) * 100.0;
        double capacityUtilization = BigDecimal.valueOf(rawUtilization).setScale(1, RoundingMode.HALF_UP).doubleValue();

        String status;
        if (capacityUtilization <= 80.0) {
            status = "NORMAL";
        } else if (capacityUtilization <= 100.0) {
            status = "OPTIMAL";
        } else if (capacityUtilization <= 125.0) {
            status = "HIGH";
        } else {
            status = "OVERLOADED";
        }

        return new EmployeeWorkloadDto(
                emp.getId(),
                emp.getName(),
                emp.getEmail(),
                emp.getDepartment() != null ? emp.getDepartment() : "General",
                total,
                open,
                completed,
                overdue,
                dueSoon,
                estimatedTotal,
                estimatedRemaining,
                actual,
                STANDARD_WEEKLY_CAPACITY_HOURS,
                capacityUtilization,
                status
        );
    }

    private List<DepartmentWorkloadDto> computeDepartmentWorkloads(List<Employee> employees,
                                                                   List<EmployeeWorkloadDto> employeeWorkloads) {
        Map<String, List<EmployeeWorkloadDto>> byDept = employeeWorkloads.stream()
                .collect(Collectors.groupingBy(EmployeeWorkloadDto::department));

        List<DepartmentWorkloadDto> list = new ArrayList<>();
        for (Map.Entry<String, List<EmployeeWorkloadDto>> entry : byDept.entrySet()) {
            String deptName = entry.getKey();
            List<EmployeeWorkloadDto> deptEmps = entry.getValue();

            int empCount = deptEmps.size();
            int totalTasks = deptEmps.stream().mapToInt(EmployeeWorkloadDto::totalTasks).sum();
            int openTasks = deptEmps.stream().mapToInt(EmployeeWorkloadDto::openTasks).sum();
            int overdueTasks = deptEmps.stream().mapToInt(EmployeeWorkloadDto::overdueTasks).sum();
            int totalEst = deptEmps.stream().mapToInt(EmployeeWorkloadDto::estimatedHoursTotal).sum();
            int totalAct = deptEmps.stream().mapToInt(EmployeeWorkloadDto::actualHours).sum();

            double avgUtil = deptEmps.isEmpty() ? 0.0 :
                    BigDecimal.valueOf(deptEmps.stream().mapToDouble(EmployeeWorkloadDto::capacityUtilization).average().orElse(0.0))
                            .setScale(1, RoundingMode.HALF_UP).doubleValue();

            list.add(new DepartmentWorkloadDto(
                    deptName,
                    empCount,
                    totalTasks,
                    openTasks,
                    overdueTasks,
                    totalEst,
                    totalAct,
                    avgUtil
            ));
        }

        list.sort(Comparator.comparingInt(DepartmentWorkloadDto::openTasks).reversed());
        return list;
    }

    private List<ProjectWorkloadDto> computeProjectWorkloads(List<Project> projects, List<Task> allTasks, LocalDate today) {
        Map<UUID, List<Task>> tasksByProject = allTasks.stream()
                .collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<ProjectWorkloadDto> list = new ArrayList<>();
        for (Project p : projects) {
            List<Task> pTasks = tasksByProject.getOrDefault(p.getId(), List.of());
            int total = pTasks.size();
            int open = 0;
            int completed = 0;
            int overdue = 0;
            int estHours = 0;
            int actHours = 0;

            for (Task t : pTasks) {
                boolean isDone = t.getStatus() == TaskStatus.DONE;
                if (isDone) {
                    completed++;
                } else {
                    open++;
                    if (t.getDueDate() != null && t.getDueDate().isBefore(today)) {
                        overdue++;
                    }
                }
                if (t.getEstimatedHours() != null) estHours += t.getEstimatedHours();
                if (t.getActualHours() != null) actHours += t.getActualHours();
            }

            double progress = total > 0 ?
                    BigDecimal.valueOf((completed * 100.0) / total).setScale(1, RoundingMode.HALF_UP).doubleValue() : 0.0;

            list.add(new ProjectWorkloadDto(
                    p.getId(),
                    p.getName(),
                    p.getStatus().name(),
                    total,
                    open,
                    completed,
                    overdue,
                    estHours,
                    actHours,
                    progress
            ));
        }

        list.sort(Comparator.comparingInt(ProjectWorkloadDto::openTasks).reversed());
        return list;
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
}
