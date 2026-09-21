package com.yourco.saas.integration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.bulk.dto.BulkOperationResultDto;
import com.yourco.saas.bulk.dto.BulkTaskStatusRequest;
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
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.search.dto.SearchResultDto;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class V1_1_MultiTenantIsolationVerificationTest extends IntegrationTestBase {

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
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private User setupUser(TenantRecord tenant, String email, Role role, String name) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            User saved = userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Architect");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            employeeRepository.save(emp);

            return saved;
        } finally {
            TenantContext.clear();
        }
    }

    private String login(TenantRecord tenant, String email) {
        ResponseEntity<TokenResponse> res = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        return res.getBody().accessToken();
    }

    @Test
    @DisplayName("Comprehensive Multi-Tenant Isolation: Tenant A cannot access Tenant B via REST, Search, Reports, Exports, or Bulk APIs")
    void strictMultiTenantIsolationAcrossAllV11Surfaces() {
        TenantRecord tenantA = provisionTenant("iso-v11-a");
        TenantRecord tenantB = provisionTenant("iso-v11-b");

        User adminA = setupUser(tenantA, "admin@tenant-a.com", Role.ADMIN, "Admin Alpha");
        User adminB = setupUser(tenantB, "admin@tenant-b.com", Role.ADMIN, "Admin Beta");

        String tokenA = login(tenantA, "admin@tenant-a.com");
        String tokenB = login(tenantB, "admin@tenant-b.com");

        UUID leaveBId;
        UUID taskBId;

        // Populate Tenant B with sensitive entities
        TenantContext.setTenant(tenantB.schemaName());
        try {
            Project projB = new Project("Beta Secret Project", "Beta Confidential", "Client Beta",
                    ProjectStatus.ACTIVE, ProjectPriority.HIGH, LocalDate.now(), null, null, null);
            projectRepository.save(projB);

            Task taskB = new Task(projB, "Beta Proprietary Algorithm", "Classified", TaskStatus.TODO,
                    TaskPriority.HIGH, adminB.getId(), "Admin Beta", adminB.getEmail(), null, 20, 0, null, "Admin Beta");
            taskBId = taskRepository.save(taskB).getId();

            LeaveRequest leaveB = new LeaveRequest(adminB.getId(), null, "Admin Beta", adminB.getEmail(),
                    LeaveType.ANNUAL, LocalDate.now().plusDays(7), LocalDate.now().plusDays(10),
                    new BigDecimal("4.0"), "Classified travel to Switzerland", LeaveStatus.PENDING);
            leaveBId = leaveRequestRepository.save(leaveB).getId();
        } finally {
            TenantContext.clear();
        }

        // 1. Direct REST Access Isolation: Tenant A tries to GET Tenant B leave request -> 404
        ResponseEntity<String> restRes = restTemplate.exchange(
                "/api/leave/" + leaveBId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, restRes.getStatusCode(),
                "Tenant A querying Tenant B leave request directly must return 404 Not Found");

        // 2. Search Isolation: Tenant A searches for "Switzerland" or "Proprietary Algorithm" -> 0 results
        ResponseEntity<List<SearchResultDto>> searchRes = restTemplate.exchange(
                "/api/search?q=Proprietary",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, searchRes.getStatusCode());
        assertTrue(searchRes.getBody().isEmpty(),
                "Tenant A search must never return Tenant B project/task");

        ResponseEntity<List<SearchResultDto>> searchLeaveRes = restTemplate.exchange(
                "/api/search?q=Switzerland",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, searchLeaveRes.getStatusCode());
        assertTrue(searchLeaveRes.getBody().isEmpty(),
                "Tenant A search must never return Tenant B leave requests");

        // 3. Reports Isolation: Tenant A reports must contain 0 tasks and 0 leave requests
        ResponseEntity<String> reportRes = restTemplate.exchange(
                "/api/reports/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, reportRes.getStatusCode());
        assertFalse(reportRes.getBody().contains("Beta Proprietary Algorithm"),
                "Tenant A tasks report must never include Tenant B tasks");

        // 4. Export Isolation: Tenant A export CSV must not contain Tenant B data
        ResponseEntity<String> exportRes = restTemplate.exchange(
                "/api/reports/export/leave",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, exportRes.getStatusCode());
        assertFalse(exportRes.getBody().contains("admin@tenant-b.com"),
                "Tenant A leave export must never contain Tenant B employee");

        // 5. Bulk API Isolation: Tenant A attempts bulk status update including Tenant B task ID
        BulkTaskStatusRequest bulkReq = new BulkTaskStatusRequest(List.of(taskBId), TaskStatus.DONE);
        ResponseEntity<BulkOperationResultDto> bulkRes = restTemplate.exchange(
                "/api/bulk/tasks/status",
                HttpMethod.POST,
                new HttpEntity<>(bulkReq, bearerHeaders(tokenA)),
                BulkOperationResultDto.class
        );
        assertEquals(HttpStatus.OK, bulkRes.getStatusCode());
        BulkOperationResultDto bulkResult = bulkRes.getBody();
        assertNotNull(bulkResult);
        assertEquals(0, bulkResult.successCount(), "Tenant A cannot modify Tenant B task via bulk API");
        assertEquals(1, bulkResult.failureCount());
        assertTrue(bulkResult.errors().get(0).error().contains("Task not found"));

        // Verify taskB in Tenant B remains in status TODO and unchanged
        TenantContext.setTenant(tenantB.schemaName());
        try {
            assertEquals(TaskStatus.TODO, taskRepository.findById(taskBId).orElseThrow().getStatus());
        } finally {
            TenantContext.clear();
        }
    }
}
