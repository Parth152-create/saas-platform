package com.yourco.saas.bulk;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.bulk.dto.BulkOperationResultDto;
import com.yourco.saas.bulk.dto.BulkTaskAssignRequest;
import com.yourco.saas.bulk.dto.BulkTaskStatusRequest;
import com.yourco.saas.bulk.dto.BulkUserStatusRequest;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class BulkOperationsIntegrationTest extends IntegrationTestBase {

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
    AuditLogRepository auditLogRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private User setupUser(TenantRecord tenant, String email, Role role) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            User saved = userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(email.split("@")[0]);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Engineer");
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
    @DisplayName("Bulk task status update allows assigned tasks and rejects unassigned for regular users")
    void bulkTaskStatusUpdatePartialFailure() {
        TenantRecord tenant = provisionTenant("bulk-status");
        User dev1 = setupUser(tenant, "dev1@bulk.test", Role.USER);
        User dev2 = setupUser(tenant, "dev2@bulk.test", Role.USER);

        String dev1Token = login(tenant, "dev1@bulk.test");

        UUID t1Id, t2Id;
        TenantContext.setTenant(tenant.schemaName());
        try {
            Project p = new Project("Bulk Project", "Desc", "Client", ProjectStatus.ACTIVE, ProjectPriority.MEDIUM, null, null, null, null);
            projectRepository.save(p);

            Task t1 = new Task(p, "Task 1 Dev 1", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, dev1.getId(), "Dev 1", dev1.getEmail(), null, 4, 0, null, "Mgr");
            t1Id = taskRepository.save(t1).getId();

            Task t2 = new Task(p, "Task 2 Dev 2", "Desc", TaskStatus.TODO, TaskPriority.MEDIUM, dev2.getId(), "Dev 2", dev2.getEmail(), null, 4, 0, null, "Mgr");
            t2Id = taskRepository.save(t2).getId();
        } finally {
            TenantContext.clear();
        }

        // Dev1 attempts to update both t1 (theirs) and t2 (dev2's) to IN_PROGRESS
        BulkTaskStatusRequest req = new BulkTaskStatusRequest(List.of(t1Id, t2Id), TaskStatus.IN_PROGRESS);
        ResponseEntity<BulkOperationResultDto> res = restTemplate.exchange(
                "/api/bulk/tasks/status",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(dev1Token)),
                BulkOperationResultDto.class
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        BulkOperationResultDto result = res.getBody();
        assertNotNull(result);
        assertEquals(2, result.totalRequested());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(List.of(t1Id), result.successIds());
        assertEquals(1, result.errors().size());
        assertEquals(t2Id, result.errors().get(0).id());
        assertTrue(result.errors().get(0).error().contains("Unauthorized"));

        // Verify t1 was updated and t2 remains TODO
        TenantContext.setTenant(tenant.schemaName());
        try {
            assertEquals(TaskStatus.IN_PROGRESS, taskRepository.findById(t1Id).orElseThrow().getStatus());
            assertEquals(TaskStatus.TODO, taskRepository.findById(t2Id).orElseThrow().getStatus());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Bulk task assignment allows Manager to assign tasks, audits action")
    void bulkTaskAssign() {
        TenantRecord tenant = provisionTenant("bulk-assign");
        User mgr = setupUser(tenant, "mgr@bulk.test", Role.MANAGER);
        User dev = setupUser(tenant, "dev@bulk.test", Role.USER);

        String mgrToken = login(tenant, "mgr@bulk.test");
        String devToken = login(tenant, "dev@bulk.test");

        UUID t1Id, t2Id;
        TenantContext.setTenant(tenant.schemaName());
        try {
            Project p = new Project("Assign Proj", "Desc", "Client", ProjectStatus.ACTIVE, ProjectPriority.MEDIUM, null, null, null, null);
            projectRepository.save(p);

            Task t1 = new Task(p, "Task A", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, null, null, null, 2, 0, null, "Mgr");
            t1Id = taskRepository.save(t1).getId();

            Task t2 = new Task(p, "Task B", "Desc", TaskStatus.TODO, TaskPriority.LOW, null, null, null, null, 4, 0, null, "Mgr");
            t2Id = taskRepository.save(t2).getId();
        } finally {
            TenantContext.clear();
        }

        // Regular user cannot bulk assign (Requires MANAGER -> 403)
        BulkTaskAssignRequest devAttempt = new BulkTaskAssignRequest(List.of(t1Id, t2Id), dev.getId());
        ResponseEntity<String> forbiddenRes = restTemplate.exchange(
                "/api/bulk/tasks/assign",
                HttpMethod.POST,
                new HttpEntity<>(devAttempt, bearerHeaders(devToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, forbiddenRes.getStatusCode());

        // Manager assigns tasks
        BulkTaskAssignRequest mgrReq = new BulkTaskAssignRequest(List.of(t1Id, t2Id), dev.getId());
        ResponseEntity<BulkOperationResultDto> res = restTemplate.exchange(
                "/api/bulk/tasks/assign",
                HttpMethod.POST,
                new HttpEntity<>(mgrReq, bearerHeaders(mgrToken)),
                BulkOperationResultDto.class
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        BulkOperationResultDto result = res.getBody();
        assertNotNull(result);
        assertEquals(2, result.totalRequested());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());

        TenantContext.setTenant(tenant.schemaName());
        try {
            Task updatedT1 = taskRepository.findById(t1Id).orElseThrow();
            assertEquals(dev.getId(), updatedT1.getAssigneeId());
            assertEquals(dev.getEmail(), updatedT1.getAssigneeEmail());

            List<AuditLog> logs = auditLogRepository.findAll();
            assertTrue(logs.stream().anyMatch(a -> "BULK_TASKS_ASSIGNED".equals(a.getAction())));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Bulk user status prevents deactivating oneself and enforces ADMIN role")
    void bulkUserStatusManagement() {
        TenantRecord tenant = provisionTenant("bulk-users");
        User admin = setupUser(tenant, "admin@bulk.test", Role.ADMIN);
        User target1 = setupUser(tenant, "target1@bulk.test", Role.USER);
        User target2 = setupUser(tenant, "target2@bulk.test", Role.USER);

        String adminToken = login(tenant, "admin@bulk.test");

        // Admin deactivates target1 and target2, plus mistakenly includes own ID
        BulkUserStatusRequest req = new BulkUserStatusRequest(
                List.of(target1.getId(), target2.getId(), admin.getId()),
                UserStatus.DISABLED
        );

        ResponseEntity<BulkOperationResultDto> res = restTemplate.exchange(
                "/api/bulk/users/status",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(adminToken)),
                BulkOperationResultDto.class
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        BulkOperationResultDto result = res.getBody();
        assertNotNull(result);
        assertEquals(3, result.totalRequested());
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertTrue(result.errors().get(0).error().contains("Cannot deactivate current user"));

        TenantContext.setTenant(tenant.schemaName());
        try {
            assertEquals(UserStatus.DISABLED, userRepository.findById(target1.getId()).orElseThrow().getStatus());
            assertEquals(UserStatus.DISABLED, userRepository.findById(target2.getId()).orElseThrow().getStatus());
            assertEquals(UserStatus.ACTIVE, userRepository.findById(admin.getId()).orElseThrow().getStatus());
        } finally {
            TenantContext.clear();
        }
    }
}
