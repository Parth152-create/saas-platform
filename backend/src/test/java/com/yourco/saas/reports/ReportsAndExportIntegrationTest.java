package com.yourco.saas.reports;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.leave.LeaveRequest;
import com.yourco.saas.domain.leave.LeaveRequestRepository;
import com.yourco.saas.domain.leave.LeaveStatus;
import com.yourco.saas.domain.leave.LeaveType;
import com.yourco.saas.domain.projects.*;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.reports.dto.LeaveReportDto;
import com.yourco.saas.reports.dto.ProjectsReportDto;
import com.yourco.saas.reports.dto.TasksReportDto;
import com.yourco.saas.reports.dto.WorkforceReportDto;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class ReportsAndExportIntegrationTest extends IntegrationTestBase {

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
    LeaveRequestRepository leaveRequestRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

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
    @DisplayName("Reports aggregate workforce, projects, tasks, leave and export CSV cleanly")
    void reportAggregationAndCsvExport() {
        TenantRecord tenant = provisionTenant("reports-export");
        String mgrToken = createAndLoginUser(tenant, "mgr@reports.test", Role.MANAGER);
        String userToken = createAndLoginUser(tenant, "user@reports.test", Role.USER);

        // Seed Project, Task, and Leave
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = userRepository.findByEmail("user@reports.test").orElseThrow();
            Employee emp = employeeRepository.findByEmailIgnoreCase("user@reports.test").orElseThrow();

            Project proj = new Project("Reporting Project", "Desc", "Client R", ProjectStatus.ACTIVE,
                    ProjectPriority.HIGH, LocalDate.now(), LocalDate.now().plusMonths(2), new BigDecimal("50000.00"), null);
            Project savedProj = projectRepository.save(proj);

            Task task = new Task(savedProj, "Generate Analytics", "Desc", TaskStatus.DONE, TaskPriority.HIGH,
                    emp.getId(), emp.getName(), emp.getEmail(), LocalDate.now(), 10, 10, null, "Mgr");
            taskRepository.save(task);

            LeaveRequest leave = new LeaveRequest(user.getId(), emp.getId(), emp.getName(), emp.getEmail(),
                    LeaveType.ANNUAL, LocalDate.now().plusDays(10), LocalDate.now().plusDays(14),
                    new BigDecimal("5.0"), "Vacation", LeaveStatus.APPROVED);
            leave.setReviewerName("Manager");
            leaveRequestRepository.save(leave);
        } finally {
            TenantContext.clear();
        }

        // 1. Workforce report
        ResponseEntity<WorkforceReportDto> wfRes = restTemplate.exchange(
                "/api/reports/workforce",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                WorkforceReportDto.class
        );
        assertEquals(HttpStatus.OK, wfRes.getStatusCode());
        assertNotNull(wfRes.getBody());
        assertTrue(wfRes.getBody().totalEmployees() >= 2);

        // 2. Projects report
        ResponseEntity<ProjectsReportDto> projRes = restTemplate.exchange(
                "/api/reports/projects",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                ProjectsReportDto.class
        );
        assertEquals(HttpStatus.OK, projRes.getStatusCode());
        assertNotNull(projRes.getBody());
        assertEquals(1, projRes.getBody().totalProjects());
        assertEquals(100.0, projRes.getBody().projects().get(0).progressPercentage());

        // 3. Tasks report
        ResponseEntity<TasksReportDto> taskRes = restTemplate.exchange(
                "/api/reports/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                TasksReportDto.class
        );
        assertEquals(HttpStatus.OK, taskRes.getStatusCode());
        assertNotNull(taskRes.getBody());
        assertEquals(1, taskRes.getBody().totalTasks());
        assertEquals(100.0, taskRes.getBody().completionRate());

        // 4. Leave report
        ResponseEntity<LeaveReportDto> leaveRes = restTemplate.exchange(
                "/api/reports/leave",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                LeaveReportDto.class
        );
        assertEquals(HttpStatus.OK, leaveRes.getStatusCode());
        assertNotNull(leaveRes.getBody());
        assertEquals(1, leaveRes.getBody().totalRequests());
        assertEquals(1, leaveRes.getBody().approvedRequests());
        assertEquals(5.0, leaveRes.getBody().totalDaysApproved().doubleValue());

        // 5. CSV Export: Workforce
        ResponseEntity<String> csvWf = restTemplate.exchange(
                "/api/reports/export/workforce",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                String.class
        );
        assertEquals(HttpStatus.OK, csvWf.getStatusCode());
        assertTrue(csvWf.getBody().contains("Employee ID,Name,Email"));
        assertTrue(csvWf.getBody().contains("user@reports.test"));

        // 6. CSV Export: Projects
        ResponseEntity<String> csvProj = restTemplate.exchange(
                "/api/reports/export/projects",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrToken)),
                String.class
        );
        assertEquals(HttpStatus.OK, csvProj.getStatusCode());
        assertTrue(csvProj.getBody().contains("Project Name,Client,Status"));
        assertTrue(csvProj.getBody().contains("Reporting Project"));

        // 7. Verify audit event for export
        TenantContext.setTenant(tenant.schemaName());
        try {
            List<AuditLog> auditLogs = auditLogRepository.findAll();
            assertTrue(auditLogs.stream().anyMatch(a -> "REPORT_EXPORTED".equals(a.getAction())));
        } finally {
            TenantContext.clear();
        }

        // 8. Regular USER cannot access reports or exports (Requires MANAGER -> 403)
        ResponseEntity<String> userForbidden = restTemplate.exchange(
                "/api/reports/workforce",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userForbidden.getStatusCode());

        ResponseEntity<String> userExportForbidden = restTemplate.exchange(
                "/api/reports/export/workforce",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userExportForbidden.getStatusCode());
    }

    @Test
    @DisplayName("Report exports strictly never include another tenant's records")
    void reportExportTenantIsolation() {
        TenantRecord tenantA = provisionTenant("rep-iso-a");
        TenantRecord tenantB = provisionTenant("rep-iso-b");

        String mgrA = createAndLoginUser(tenantA, "mgra@tenant-a.com", Role.MANAGER);
        createAndLoginUser(tenantB, "secret-user@tenant-b.com", Role.USER);

        // Export Tenant A workforce
        ResponseEntity<String> csvA = restTemplate.exchange(
                "/api/reports/export/workforce",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(mgrA)),
                String.class
        );
        assertEquals(HttpStatus.OK, csvA.getStatusCode());
        assertFalse(csvA.getBody().contains("secret-user@tenant-b.com"),
                "Tenant A export must NEVER contain Tenant B user records");
    }
}
