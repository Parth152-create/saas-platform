package com.yourco.saas.projects;

import com.stripe.model.checkout.Session;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.SignupRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.StripeCheckoutSessionCreator;
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
import com.yourco.saas.hrm.dto.CreateEmployeeRequest;
import com.yourco.saas.hrm.dto.EmployeeDto;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class ProjectTaskFinalBusinessFlowTest extends IntegrationTestBase {

    private static final String PASSWORD = "SecurePassword123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    StripeCheckoutSessionCreator checkoutSessionCreator;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("Complete 24-Step Business Flow Verification")
    void verifyComplete24StepBusinessFlow() {
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        String tenantAId = "tenant-a-" + suffixA;
        String superAdminAEmail = "superadmin-" + suffixA + "@alpha.io";

        // 1. Register Tenant A
        SignupRequest signupReqA = new SignupRequest(tenantAId, superAdminAEmail, PASSWORD);
        ResponseEntity<TokenResponse> signupResA = restTemplate.postForEntity(
                "/api/auth/signup", signupReqA, TokenResponse.class);
        assertEquals(HttpStatus.CREATED, signupResA.getStatusCode());
        assertNotNull(signupResA.getBody());
        trackProvisionedSchema("tenant_" + tenantAId.replace("-", "_"));

        // 2. Login as SUPER_ADMIN
        ResponseEntity<TokenResponse> loginResA = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantAId, superAdminAEmail, PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResA.getStatusCode());
        assertNotNull(loginResA.getBody());
        String superAdminTokenA = loginResA.getBody().accessToken();

        // 3. Create/invite employees
        CreateEmployeeRequest empReq1 = new CreateEmployeeRequest(
                "EMP-101", "Marcus Sterling", "marcus-" + suffixA + "@alpha.io",
                "Operations", "Senior Operations Lead", EmployeeStatus.ACTIVE,
                LocalDate.now(), "+1-555-101", "Hybrid", "Austin, TX", "Alexander Chen",
                "bg-zinc-800 text-zinc-100", new BigDecimal("100.00"), 40
        );
        ResponseEntity<EmployeeDto> empRes1 = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.POST,
                new HttpEntity<>(empReq1, bearerHeaders(superAdminTokenA)),
                EmployeeDto.class
        );
        assertEquals(HttpStatus.CREATED, empRes1.getStatusCode());
        assertNotNull(empRes1.getBody());
        UUID employee1Id = empRes1.getBody().id();

        // 4. Create Project A
        CreateProjectRequest projAReq = new CreateProjectRequest(
                "Project Alpha Core",
                "Core platform migration and scaling",
                "Acme Global",
                ProjectStatus.ACTIVE,
                ProjectPriority.HIGH,
                LocalDate.now(),
                LocalDate.now().plusDays(45),
                new BigDecimal("75000.00"),
                null,
                null
        );
        ResponseEntity<ProjectResponse> projARes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projAReq, bearerHeaders(superAdminTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projARes.getStatusCode());
        assertNotNull(projARes.getBody());
        UUID projectAId = projARes.getBody().id();
        assertEquals(0, projARes.getBody().progress());

        // 5. Assign Project A team members
        AddProjectMemberRequest addMemberReq = new AddProjectMemberRequest(employee1Id, "LEAD");
        ResponseEntity<ProjectMemberDto> memberRes = restTemplate.exchange(
                "/api/projects/" + projectAId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(addMemberReq, bearerHeaders(superAdminTokenA)),
                ProjectMemberDto.class
        );
        assertEquals(HttpStatus.CREATED, memberRes.getStatusCode());
        assertEquals("Marcus Sterling", memberRes.getBody().memberName());

        // 6. Create multiple Tasks
        CreateTaskRequest task1Req = new CreateTaskRequest(
                "Design Architecture Spec",
                "Document multi-tenant partitioning",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                null,
                LocalDate.now().plusDays(10),
                16,
                0
        );
        ResponseEntity<TaskResponse> task1Res = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(task1Req, bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, task1Res.getStatusCode());
        UUID task1Id = task1Res.getBody().id();

        CreateTaskRequest task2Req = new CreateTaskRequest(
                "Configure Flyway Migrations",
                "Apply V11 tables",
                TaskStatus.TODO,
                TaskPriority.URGENT,
                null,
                LocalDate.now().plusDays(5),
                8,
                0
        );
        ResponseEntity<TaskResponse> task2Res = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(task2Req, bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, task2Res.getStatusCode());
        UUID task2Id = task2Res.getBody().id();

        // 7. Assign Tasks
        UpdateTaskRequest assignTask1 = new UpdateTaskRequest(
                null, null, null, null, employee1Id, null, null, null
        );
        ResponseEntity<TaskResponse> assignRes = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(assignTask1, bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.OK, assignRes.getStatusCode());
        assertEquals(employee1Id, assignRes.getBody().assigneeId());

        // 8. Move tasks: TODO -> IN_PROGRESS -> REVIEW -> DONE
        // Task 1: TODO -> IN_PROGRESS
        restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null, null, null, 4), bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );
        // Task 1: IN_PROGRESS -> REVIEW
        restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskRequest(null, null, TaskStatus.REVIEW, null, null, null, null, 10), bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );
        // Task 1: REVIEW -> DONE
        restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null, null, 16), bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );

        // 9. Verify project progress changes automatically (1 of 2 tasks DONE = 50%)
        ResponseEntity<ProjectResponse> projProg50 = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superAdminTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, projProg50.getStatusCode());
        assertEquals(50, projProg50.getBody().progress());
        assertEquals(1, projProg50.getBody().doneTasks());
        assertEquals(2, projProg50.getBody().totalTasks());

        // Task 2: TODO -> DONE
        restTemplate.exchange(
                "/api/tasks/" + task2Id,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskRequest(null, null, TaskStatus.DONE, null, null, null, null, 8), bearerHeaders(superAdminTokenA)),
                TaskResponse.class
        );

        // Progress becomes 100%
        ResponseEntity<ProjectResponse> projProg100 = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superAdminTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, projProg100.getStatusCode());
        assertEquals(100, projProg100.getBody().progress());

        // 10. Verify project dashboard statistics
        ResponseEntity<ProjectStatsResponse> statsRes = restTemplate.exchange(
                "/api/projects/stats",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superAdminTokenA)),
                ProjectStatsResponse.class
        );
        assertEquals(HttpStatus.OK, statsRes.getStatusCode());
        assertNotNull(statsRes.getBody());
        assertEquals(1, statsRes.getBody().activeProjects());
        assertEquals(0, statsRes.getBody().openTasks());
        assertEquals(2, statsRes.getBody().completedTasks());

        // 11. Verify activity/audit events
        ResponseEntity<List<ProjectActivityDto>> activityRes = restTemplate.exchange(
                "/api/projects/activity",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superAdminTokenA)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, activityRes.getStatusCode());
        assertNotNull(activityRes.getBody());
        assertTrue(activityRes.getBody().stream().anyMatch(a -> a.action().contains("created project")));
        assertTrue(activityRes.getBody().stream().anyMatch(a -> a.action().contains("completed task")));

        // 12. Login as MANAGER
        String managerEmail = "manager-" + suffixA + "@alpha.io";
        TenantRecord tenantARecord = tenantRegistryService.findByTenantId(tenantAId).orElseThrow();
        TenantContext.setTenant(tenantARecord.schemaName());
        try {
            User mgr = UserTestFactory.localUser(managerEmail, Role.MANAGER);
            mgr.setPasswordHash(passwordEncoder.encode(PASSWORD));
            mgr.setStatus(UserStatus.ACTIVE);
            userRepository.save(mgr);
        } finally {
            TenantContext.clear();
        }
        ResponseEntity<TokenResponse> mgrLogin = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantAId, managerEmail, PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, mgrLogin.getStatusCode());
        String managerToken = mgrLogin.getBody().accessToken();

        // 13. Verify manager permissions: can create project, can manage task, but CANNOT delete project
        CreateProjectRequest mgrProj = new CreateProjectRequest(
                "Manager Project", "Allowed", "Client", ProjectStatus.PLANNING, ProjectPriority.LOW, null, null, null, null, null
        );
        ResponseEntity<ProjectResponse> mgrProjRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(mgrProj, bearerHeaders(managerToken)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, mgrProjRes.getStatusCode());
        UUID mgrProjId = mgrProjRes.getBody().id();

        // Manager cannot delete project
        ResponseEntity<String> mgrDeleteProjRes = restTemplate.exchange(
                "/api/projects/" + mgrProjId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(managerToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, mgrDeleteProjRes.getStatusCode());

        // 14. Login as USER
        String userEmail = "user-" + suffixA + "@alpha.io";
        TenantContext.setTenant(tenantARecord.schemaName());
        try {
            User usr = UserTestFactory.localUser(userEmail, Role.USER);
            usr.setPasswordHash(passwordEncoder.encode(PASSWORD));
            usr.setStatus(UserStatus.ACTIVE);
            userRepository.save(usr);
        } finally {
            TenantContext.clear();
        }
        ResponseEntity<TokenResponse> usrLogin = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantAId, userEmail, PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, usrLogin.getStatusCode());
        String userToken = usrLogin.getBody().accessToken();

        // 15. Verify user restrictions: cannot create project, cannot create task, cannot delete task
        ResponseEntity<String> usrCreateProj = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(mgrProj, bearerHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, usrCreateProj.getStatusCode());

        ResponseEntity<String> usrCreateTask = restTemplate.exchange(
                "/api/projects/" + projectAId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(task1Req, bearerHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, usrCreateTask.getStatusCode());

        ResponseEntity<String> usrDeleteTask = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, usrDeleteTask.getStatusCode());

        // 16. Create Tenant B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        String tenantBId = "tenant-b-" + suffixB;
        String superAdminBEmail = "superadmin-" + suffixB + "@beta.io";
        SignupRequest signupReqB = new SignupRequest(tenantBId, superAdminBEmail, PASSWORD);
        ResponseEntity<TokenResponse> signupResB = restTemplate.postForEntity(
                "/api/auth/signup", signupReqB, TokenResponse.class);
        assertEquals(HttpStatus.CREATED, signupResB.getStatusCode());
        trackProvisionedSchema("tenant_" + tenantBId.replace("-", "_"));

        String tokenB = signupResB.getBody().accessToken();

        // 17. Create Project B
        CreateProjectRequest projBReq = new CreateProjectRequest(
                "Project Beta Unique", "Tenant B only", "Client Beta", ProjectStatus.ACTIVE, ProjectPriority.MEDIUM, null, null, null, null, null
        );
        ResponseEntity<ProjectResponse> projBRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projBReq, bearerHeaders(tokenB)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projBRes.getStatusCode());
        UUID projectBId = projBRes.getBody().id();

        // 18. Attempt Tenant B -> Project A access (GET)
        ResponseEntity<String> bAccessProjA = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, bAccessProjA.getStatusCode());

        // 19. Attempt Tenant B -> Task A access (GET)
        ResponseEntity<String> bAccessTaskA = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, bAccessTaskA.getStatusCode());

        // 20. Attempt Tenant B -> modify Project A (PATCH)
        ResponseEntity<String> bModifyProjA = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateProjectRequest("Hacked Project A", null, null, null, null, null, null, null, null, null), bearerHeaders(tokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, bModifyProjA.getStatusCode());

        // 21. Attempt Tenant B -> modify Task A (PATCH)
        ResponseEntity<String> bModifyTaskA = restTemplate.exchange(
                "/api/tasks/" + task1Id,
                HttpMethod.PATCH,
                new HttpEntity<>(new UpdateTaskRequest("Hacked Task A", null, null, null, null, null, null, null), bearerHeaders(tokenB)),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, bModifyTaskA.getStatusCode());

        // 22. Verify all cross-tenant attempts are rejected (tested above, all 404 NOT_FOUND)

        // 23. Verify Tenant A data remains unchanged
        ResponseEntity<ProjectResponse> verifyProjA = restTemplate.exchange(
                "/api/projects/" + projectAId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(superAdminTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.OK, verifyProjA.getStatusCode());
        assertEquals("Project Alpha Core", verifyProjA.getBody().name());
        assertEquals(100, verifyProjA.getBody().progress());

        // 24. Verify Tenant B sees only Tenant B data
        ResponseEntity<List<ProjectResponse>> bProjects = restTemplate.exchange(
                "/api/projects",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tokenB)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, bProjects.getStatusCode());
        assertEquals(1, bProjects.getBody().size());
        assertEquals(projectBId, bProjects.getBody().get(0).id());
        assertEquals("Project Beta Unique", bProjects.getBody().get(0).name());
    }
}
