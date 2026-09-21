package com.yourco.saas.workload;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.projects.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.workload.dto.EmployeeWorkloadDto;
import com.yourco.saas.workload.dto.WorkforceWorkloadSummaryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class WorkloadIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createAndLoginUser(TenantRecord tenant, String email, Role role) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(email.split("@")[0]);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Developer");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            employeeRepository.save(emp);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> res = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        return res.getBody().accessToken();
    }

    @Test
    @DisplayName("Workload calculations correctly compute open, completed, overdue, and capacity utilization")
    void workloadCalculations() {
        TenantRecord tenant = provisionTenant("workload-calc");

        String mgrToken = createAndLoginUser(tenant, "lead@workload.test", Role.MANAGER);
        String devToken = createAndLoginUser(tenant, "dev@workload.test", Role.USER);

        // Seed tasks for dev@workload.test
        TenantContext.setTenant(tenant.schemaName());
        try {
            Employee dev = employeeRepository.findByEmailIgnoreCase("dev@workload.test").orElseThrow();
            Project proj = new Project("Nexa V1.1", "Operations", "Internal", ProjectStatus.ACTIVE,
                    ProjectPriority.HIGH, LocalDate.now(), LocalDate.now().plusMonths(1), null, null);
            Project savedProj = projectRepository.save(proj);

            // Open task (20 hours, due in 2 days -> due soon)
            Task t1 = new Task(savedProj, "Task 1", "Open", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                    dev.getId(), dev.getName(), dev.getEmail(), LocalDate.now().plusDays(2), 20, 5, null, "Lead");
            taskRepository.save(t1);

            // Overdue task (10 hours, due 3 days ago)
            Task t2 = new Task(savedProj, "Task 2", "Overdue", TaskStatus.TODO, TaskPriority.URGENT,
                    dev.getId(), dev.getName(), dev.getEmail(), LocalDate.now().minusDays(3), 10, 0, null, "Lead");
            taskRepository.save(t2);

            // Completed task (15 hours)
            Task t3 = new Task(savedProj, "Task 3", "Completed", TaskStatus.DONE, TaskPriority.MEDIUM,
                    dev.getId(), dev.getName(), dev.getEmail(), LocalDate.now().minusDays(1), 15, 15, null, "Lead");
            taskRepository.save(t3);
        } finally {
            TenantContext.clear();
        }

        // Manager queries workforce summary
        ResponseEntity<WorkforceWorkloadSummaryDto> summaryRes = restTemplate.exchange(
                "/api/workload/summary",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                WorkforceWorkloadSummaryDto.class
        );

        assertEquals(HttpStatus.OK, summaryRes.getStatusCode());
        WorkforceWorkloadSummaryDto summary = summaryRes.getBody();
        assertNotNull(summary);
        assertEquals(3, summary.totalTasks());
        assertEquals(2, summary.openTasks());
        assertEquals(1, summary.completedTasks());
        assertEquals(1, summary.overdueTasks());
        assertEquals(1, summary.dueSoonTasks());
        assertEquals(45, summary.totalEstimatedHours());
        assertEquals(20, summary.totalActualHours());

        // Check dev's individual workload: open estimated hours = 20 + 10 = 30 hours.
        // Utilization on 40h standard capacity = (30 / 40.0) * 100 = 75.0% -> status NORMAL
        EmployeeWorkloadDto devWorkload = summary.employeeWorkloads().stream()
                .filter(e -> "dev@workload.test".equals(e.email()))
                .findFirst().orElseThrow();
        assertEquals(3, devWorkload.totalTasks());
        assertEquals(2, devWorkload.openTasks());
        assertEquals(1, devWorkload.completedTasks());
        assertEquals(1, devWorkload.overdueTasks());
        assertEquals(1, devWorkload.dueSoonTasks());
        assertEquals(30, devWorkload.estimatedHoursRemaining());
        assertEquals(75.0, devWorkload.capacityUtilization());
        assertEquals("NORMAL", devWorkload.workloadStatus());

        // Developer can access GET /api/workload/my
        ResponseEntity<EmployeeWorkloadDto> myRes = restTemplate.exchange(
                "/api/workload/my",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(devToken)),
                EmployeeWorkloadDto.class
        );
        assertEquals(HttpStatus.OK, myRes.getStatusCode());
        assertNotNull(myRes.getBody());
        assertEquals(75.0, myRes.getBody().capacityUtilization());

        // Developer cannot access workforce summary (Requires MANAGER -> 403)
        ResponseEntity<String> forbiddenRes = restTemplate.exchange(
                "/api/workload/summary",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(devToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenRes.getStatusCode());
    }

    @Test
    @DisplayName("Workload respects tenant isolation: Tenant A workload does not count Tenant B tasks")
    void workloadTenantIsolation() {
        TenantRecord tenantA = provisionTenant("workload-iso-a");
        TenantRecord tenantB = provisionTenant("workload-iso-b");

        String mgrA = createAndLoginUser(tenantA, "mgr@tenant-a.com", Role.MANAGER);
        createAndLoginUser(tenantB, "mgr@tenant-b.com", Role.MANAGER);

        // Add 5 tasks to Tenant B
        TenantContext.setTenant(tenantB.schemaName());
        try {
            Employee empB = employeeRepository.findByEmailIgnoreCase("mgr@tenant-b.com").orElseThrow();
            Project projB = new Project("Project B", "Desc", "Client B", ProjectStatus.ACTIVE, ProjectPriority.HIGH, null, null, null, null);
            projectRepository.save(projB);

            for (int i = 0; i < 5; i++) {
                taskRepository.save(new Task(projB, "Task " + i, "B Task", TaskStatus.TODO, TaskPriority.MEDIUM,
                        empB.getId(), empB.getName(), empB.getEmail(), null, 10, 0, null, "Mgr"));
            }
        } finally {
            TenantContext.clear();
        }

        // Query Tenant A summary
        ResponseEntity<WorkforceWorkloadSummaryDto> summaryRes = restTemplate.exchange(
                "/api/workload/summary",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrA)),
                WorkforceWorkloadSummaryDto.class
        );

        assertEquals(HttpStatus.OK, summaryRes.getStatusCode());
        assertEquals(0, summaryRes.getBody().totalTasks(), "Tenant A must have 0 tasks despite Tenant B having 5");
    }
}
