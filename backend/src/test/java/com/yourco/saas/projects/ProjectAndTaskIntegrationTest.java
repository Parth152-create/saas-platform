package com.yourco.saas.projects;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.hrm.Employee;
import com.yourco.saas.domain.hrm.EmployeeRepository;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.projects.ProjectPriority;
import com.yourco.saas.domain.projects.ProjectStatus;
import com.yourco.saas.domain.projects.TaskPriority;
import com.yourco.saas.domain.projects.TaskStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.projects.dto.*;
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
class ProjectAndTaskIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EmployeeRepository employeeRepository;

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

    private Employee createEmployee(TenantRecord tenant, String empId, String name, String email) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            Employee emp = new Employee();
            emp.setEmployeeId(empId);
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Software Architect");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            return employeeRepository.save(emp);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Project Lifecycle, Progress Calculation, Task Transitions, RBAC and Tenant Isolation")
    void testProjectAndTaskFullLifecycle() {
        // 1. Create two distinct tenants
        TenantRecord tenantA = provisionTenant("proj-a");
        TenantRecord tenantB = provisionTenant("proj-b");

        // Create users in Tenant A
        String adminTokenA = createAndLoginUser(tenantA, "admin@tenanta.com", Role.ADMIN);
        String managerTokenA = createAndLoginUser(tenantA, "manager@tenanta.com", Role.MANAGER);
        String userTokenA = createAndLoginUser(tenantA, "user@tenanta.com", Role.USER);
        Employee empA = createEmployee(tenantA, "EMP-A-01", "Alice Developer", "alice@tenanta.com");

        // Create user & employee in Tenant B
        String adminTokenB = createAndLoginUser(tenantB, "admin@tenantb.com", Role.ADMIN);
        Employee empB = createEmployee(tenantB, "EMP-B-01", "Bob Engineer", "bob@tenantb.com");

        // ---------------------------------------------------------------------
        // 2. Project Creation & RBAC in Tenant A
        // ---------------------------------------------------------------------
        // USER cannot create a project -> 403 Forbidden
        CreateProjectRequest reqA = new CreateProjectRequest(
                "Nexa Migration",
                "Cloud infrastructure upgrade",
                "Enterprise Customer",
                ProjectStatus.ACTIVE,
                ProjectPriority.HIGH,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                new BigDecimal("50000.00"),
                null,
                List.of(empA.getId())
        );

        ResponseEntity<String> userCreateProjRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(reqA, bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userCreateProjRes.getStatusCode());

        // Cross-tenant assignment: Manager in Tenant A tries to assign empB -> 400 Bad Request
        CreateProjectRequest crossTenantReq = new CreateProjectRequest(
                "Cross Tenant Test",
                "Should fail",
                "None",
                ProjectStatus.PLANNING,
                ProjectPriority.LOW,
                null, null, null, null,
                List.of(empB.getId()) // empB belongs to Tenant B!
        );
        ResponseEntity<String> crossAssignRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(crossTenantReq, bearerHeaders(managerTokenA)),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, crossAssignRes.getStatusCode());

        // MANAGER creates Project A successfully
        ResponseEntity<ProjectResponse> createProjRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(reqA, bearerHeaders(managerTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, createProjRes.getStatusCode());
        assertNotNull(createProjRes.getBody());
        UUID projectAId = createProjRes.getBody().id();
        assertEquals("Nexa Migration", createProjRes.getBody().name());
        assertEquals(0, createProjRes.getBody().progress()); // 0 tasks -> 0%
        assertEquals(1, createProjRes.getBody().teamMemberCount());

        // ---------------------------------------------------------------------
        // 3. Task Creation & Dynamic Progress in Tenant A
        // ---------------------------------------------------------------------
        // USER cannot create a task -> 403 Forbidden
        CreateTaskRequest taskReq1 = new CreateTaskRequest(
                "Setup Postgres schemas",
                "Run Flyway migrations",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                empA.getId(),
                LocalDate.now().plusDays(5),
                8,
                0
        );
        ResponseEntity<String> userCreateTaskRes = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq1, bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userCreateTaskRes.getStatusCode());

        // Cross-tenant task assignment: Manager in Tenant A tries to assign empB -> 400 Bad Request
        CreateTaskRequest crossTaskReq = new CreateTaskRequest(
                "Invalid task",
                "Cross tenant assignee",
                TaskStatus.TODO,
                TaskPriority.LOW,
                empB.getId(), // Tenant B employee
                LocalDate.now().plusDays(5),
                4,
                0
        );
        ResponseEntity<String> crossTaskAssignRes = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(crossTaskReq, bearerHeaders(managerTokenA)),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, crossTaskAssignRes.getStatusCode());

        // MANAGER creates Task 1 (TODO)
        ResponseEntity<TaskResponse> task1Res = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq1, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, task1Res.getStatusCode());
        assertNotNull(task1Res.getBody());
        UUID task1Id = task1Res.getBody().id();
        assertEquals("Setup Postgres schemas", task1Res.getBody().title());
        assertEquals(TaskStatus.TODO, task1Res.getBody().status());
        assertEquals(empA.getId(), task1Res.getBody().assigneeId());

        // MANAGER creates Task 2 (Overdue & IN_PROGRESS)
        CreateTaskRequest taskReq2 = new CreateTaskRequest(
                "Audit Security Tokens",
                "Verify JWT revocation",
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                null,
                LocalDate.now().minusDays(2), // Overdue!
                4,
                2
        );
        ResponseEntity<TaskResponse> task2Res = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq2, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, task2Res.getStatusCode());
        UUID task2Id = task2Res.getBody().id();

        // Check project progress after 2 tasks (0 DONE / 2 total = 0%)
        ResponseEntity<ProjectResponse> projAfterTasks = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, projAfterTasks.getStatusCode());
        assertEquals(2, projAfterTasks.getBody().totalTasks());
        assertEquals(0, projAfterTasks.getBody().doneTasks());
        assertEquals(1, projAfterTasks.getBody().overdueTasks());
        assertEquals(0, projAfterTasks.getBody().progress());

        // ---------------------------------------------------------------------
        // 4. Task Status Transitions & Progress Update
        // ---------------------------------------------------------------------
        // Move Task 1: TODO -> IN_PROGRESS
        UpdateTaskRequest updateTask1_1 = new UpdateTaskRequest(
                null, null, TaskStatus.IN_PROGRESS, null, null, null, null, 3
        );
        ResponseEntity<TaskResponse> updateTask1Res = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(updateTask1_1, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.OK, updateTask1Res.getStatusCode());
        assertEquals(TaskStatus.IN_PROGRESS, updateTask1Res.getBody().status());

        // Move Task 1: IN_PROGRESS -> REVIEW
        UpdateTaskRequest updateTask1_2 = new UpdateTaskRequest(
                null, null, TaskStatus.REVIEW, null, null, null, null, 6
        );
        restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(updateTask1_2, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );

        // Move Task 1: REVIEW -> DONE
        UpdateTaskRequest updateTask1_3 = new UpdateTaskRequest(
                null, null, TaskStatus.DONE, null, null, null, null, 8
        );
        restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(updateTask1_3, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );

        // Verify project progress is now 50% (1 of 2 tasks DONE)
        ResponseEntity<ProjectResponse> projProgress50 = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, projProgress50.getStatusCode());
        assertEquals(1, projProgress50.getBody().doneTasks());
        assertEquals(50, projProgress50.getBody().progress());

        // Move Task 2: IN_PROGRESS -> DONE
        UpdateTaskRequest updateTask2 = new UpdateTaskRequest(
                null, null, TaskStatus.DONE, null, null, null, null, 4
        );
        restTemplate.exchange(
                "/api/tasks/" + task2Id,
                HttpMethod.PATCH,
                new HttpEntity<>(updateTask2, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );

        // Verify project progress is now 100% (2 of 2 tasks DONE)
        ResponseEntity<ProjectResponse> projProgress100 = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, projProgress100.getStatusCode());
        assertEquals(2, projProgress100.getBody().doneTasks());
        assertEquals(100, projProgress100.getBody().progress());

        // ---------------------------------------------------------------------
        // 5. Dashboard Stats & Activity Feed
        // ---------------------------------------------------------------------
        ResponseEntity<ProjectStatsResponse> statsRes = restTemplate.exchange(
                "/api/projects/stats",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                ProjectStatsResponse.class
        );
        assertEquals(HttpStatus.OK, statsRes.getStatusCode());
        assertNotNull(statsRes.getBody());
        assertEquals(1, statsRes.getBody().activeProjects());
        assertEquals(1, statsRes.getBody().totalProjects());
        assertEquals(0, statsRes.getBody().openTasks());
        assertEquals(2, statsRes.getBody().completedTasks());

        // Global activity
        ResponseEntity<List<ProjectActivityDto>> actRes = restTemplate.exchange(
                "/api/projects/activity",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, actRes.getStatusCode());
        assertNotNull(actRes.getBody());
        assertFalse(actRes.getBody().isEmpty());

        // Project specific activity
        ResponseEntity<List<ProjectActivityDto>> projActRes = restTemplate.exchange(
                "/api/projects/" + projectAId + "/activity",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, projActRes.getStatusCode());
        assertNotNull(projActRes.getBody());
        assertFalse(projActRes.getBody().isEmpty());

        // ---------------------------------------------------------------------
        // 6. Cross-Tenant Isolation Enforcement (Critical)
        // ---------------------------------------------------------------------
        // Tenant B attempting to read Project A -> 404
        ResponseEntity<String> crossGetProj = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossGetProj.getStatusCode());

        // Tenant B attempting to update Project A -> 404
        UpdateProjectRequest maliciousUpdate = new UpdateProjectRequest(
                "Hacked Name", null, null, null, null, null, null, null, null, null
        );
        ResponseEntity<String> crossPatchProj = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.PATCH,
                new HttpEntity<>(maliciousUpdate, bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossPatchProj.getStatusCode());

        // Tenant B attempting to delete Project A -> 404
        ResponseEntity<String> crossDeleteProj = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossDeleteProj.getStatusCode());

        // Tenant B attempting to read tasks for Project A -> 404
        ResponseEntity<String> crossGetTasks = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossGetTasks.getStatusCode());

        // Tenant B attempting to read Task A directly -> 404
        ResponseEntity<String> crossGetTask = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossGetTask.getStatusCode());

        // Tenant B attempting to patch Task A directly -> 404
        ResponseEntity<String> crossPatchTask = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(updateTask1_1, bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossPatchTask.getStatusCode());

        // Tenant B attempting to delete Task A directly -> 404
        ResponseEntity<String> crossDeleteTask = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, crossDeleteTask.getStatusCode());

        // Verify Tenant A data remained intact after all Tenant B attempts
        ResponseEntity<ProjectResponse> verifyProjA = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, verifyProjA.getStatusCode());
        assertEquals("Nexa Migration", verifyProjA.getBody().name());

        // ---------------------------------------------------------------------
        // 7. Deletion RBAC in Tenant A
        // ---------------------------------------------------------------------
        // MANAGER cannot delete project -> 403 Forbidden
        ResponseEntity<String> managerDeleteRes = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(managerTokenA)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, managerDeleteRes.getStatusCode());

        // ADMIN can delete task 2
        ResponseEntity<Void> deleteTaskRes = restTemplate.exchange(
                "/api/tasks/" + task2Id,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteTaskRes.getStatusCode());

        // ADMIN can delete project
        ResponseEntity<Void> adminDeleteRes = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, adminDeleteRes.getStatusCode());

        // Verify project is gone -> 404
        ResponseEntity<String> getDeletedRes = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenA)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getDeletedRes.getStatusCode());
    }

    @Test
    @DisplayName("Project Members, Search/Filtering, and Granular User Permissions")
    void testRegularUserRestrictionsAndMemberManagement() {
        TenantRecord tenant = provisionTenant("proj-details");
        String adminToken = createAndLoginUser(tenant, "admin@test.com", Role.ADMIN);
        String managerToken = createAndLoginUser(tenant, "manager@test.com", Role.MANAGER);
        String user1Token = createAndLoginUser(tenant, "user1@test.com", Role.USER);
        String user2Token = createAndLoginUser(tenant, "user2@test.com", Role.USER);

        Employee emp1 = createEmployee(tenant, "EMP-101", "Member One", "user1@test.com");
        Employee emp2 = createEmployee(tenant, "EMP-102", "Member Two", "user2@test.com");

        // Create a project
        CreateProjectRequest req = new CreateProjectRequest(
                "Internal Portal",
                "Portal rewrite",
                "Internal",
                ProjectStatus.PLANNING,
                ProjectPriority.MEDIUM,
                LocalDate.now(),
                LocalDate.now().plusDays(14),
                new BigDecimal("25000.00"),
                null,
                null
        );
        ResponseEntity<ProjectResponse> projRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(managerToken)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projRes.getStatusCode());
        UUID projId = projRes.getBody().id();

        // Add member
        AddProjectMemberRequest addMemberReq = new AddProjectMemberRequest(emp1.getId(), "DEVELOPER");
        ResponseEntity<ProjectMemberDto> addMemberRes = restTemplate.exchange(
                "/api/projects/" + projId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(addMemberReq, bearerHeaders(managerToken)),
                ProjectMemberDto.class
        );
        assertEquals(HttpStatus.CREATED, addMemberRes.getStatusCode());
        assertEquals("Member One", addMemberRes.getBody().memberName());

        // List members
        ResponseEntity<List<ProjectMemberDto>> listMembersRes = restTemplate.exchange(
                "/api/projects/" + projId + "/members",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user1Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, listMembersRes.getStatusCode());
        assertEquals(1, listMembersRes.getBody().size());

        // Create Task assigned to user1 (emp1)
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "Design Database Tables",
                "Create DDL scripts",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                emp1.getId(),
                LocalDate.now().plusDays(3),
                10,
                0
        );
        ResponseEntity<TaskResponse> taskRes = restTemplate.exchange(
                "/api/projects/" + projId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq, bearerHeaders(managerToken)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, taskRes.getStatusCode());
        UUID taskId = taskRes.getBody().id();

        // User2 attempts to update User1's task -> 403 Forbidden
        UpdateTaskRequest user2Update = new UpdateTaskRequest(
                null, null, TaskStatus.IN_PROGRESS, null, null, null, null, 2
        );
        ResponseEntity<String> user2Fail = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.PATCH,
                new HttpEntity<>(user2Update, bearerHeaders(user2Token)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, user2Fail.getStatusCode());

        // User1 attempts to rename task -> 403 Forbidden
        UpdateTaskRequest user1BadUpdate = new UpdateTaskRequest(
                "Hacked Task Title", null, null, null, null, null, null, null
        );
        ResponseEntity<String> user1BadRes = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.PATCH,
                new HttpEntity<>(user1BadUpdate, bearerHeaders(user1Token)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, user1BadRes.getStatusCode());

        // User1 successfully updates their own task status & actual hours
        UpdateTaskRequest user1OkUpdate = new UpdateTaskRequest(
                null, null, TaskStatus.IN_PROGRESS, null, null, null, null, 5
        );
        ResponseEntity<TaskResponse> user1OkRes = restTemplate.exchange(
                "/api/tasks/" + taskId,
                HttpMethod.PATCH,
                new HttpEntity<>(user1OkUpdate, bearerHeaders(user1Token)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.OK, user1OkRes.getStatusCode());
        assertEquals(TaskStatus.IN_PROGRESS, user1OkRes.getBody().status());
        assertEquals(5, user1OkRes.getBody().actualHours());

        // Search & filtering tests
        ResponseEntity<List<ProjectResponse>> searchProjects = restTemplate.exchange(
                "/api/projects?search=Portal",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user1Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, searchProjects.getStatusCode());
        assertEquals(1, searchProjects.getBody().size());

        ResponseEntity<List<TaskResponse>> searchTasks = restTemplate.exchange(
                "/api/tasks?search=Database",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(user1Token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, searchTasks.getStatusCode());
        assertEquals(1, searchTasks.getBody().size());

        // Remove member
        ResponseEntity<Void> removeMemberRes = restTemplate.exchange(
                "/api/projects/" + projId + "/members/" + emp1.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(managerToken)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, removeMemberRes.getStatusCode());
    }
}

