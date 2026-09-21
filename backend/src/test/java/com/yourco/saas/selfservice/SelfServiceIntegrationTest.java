package com.yourco.saas.selfservice;

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
import com.yourco.saas.projects.dto.ProjectResponse;
import com.yourco.saas.projects.dto.TaskResponse;
import com.yourco.saas.selfservice.dto.SelfServiceOverviewDto;
import com.yourco.saas.selfservice.dto.SelfServiceProfileDto;
import com.yourco.saas.selfservice.dto.UpdateSelfServiceProfileRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class SelfServiceIntegrationTest extends IntegrationTestBase {

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
    ProjectMemberRepository projectMemberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private User setupUserAndEmployee(TenantRecord tenant, String email, Role role, String name) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            User savedUser = userRepository.save(user);

            Employee emp = new Employee();
            emp.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 6));
            emp.setName(name);
            emp.setEmail(email);
            emp.setDepartment("Engineering");
            emp.setPosition("Senior Engineer");
            emp.setStatus(EmployeeStatus.ACTIVE);
            emp.setHireDate(LocalDate.now());
            emp.setPhone("+1555123456");
            emp.setLocation("San Francisco, CA");
            emp.setWorkModel("Hybrid");
            employeeRepository.save(emp);

            return savedUser;
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
    @DisplayName("Employee can access own profile and update permitted fields")
    void employeeSelfServiceProfile() {
        TenantRecord tenant = provisionTenant("ss-profile");
        setupUserAndEmployee(tenant, "user@selfservice.test", Role.USER, "Alice Engineer");
        String token = login(tenant, "user@selfservice.test");

        // 1. Get profile
        ResponseEntity<SelfServiceProfileDto> getRes = restTemplate.exchange(
                "/api/self-service/profile",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                SelfServiceProfileDto.class
        );

        assertEquals(HttpStatus.OK, getRes.getStatusCode());
        SelfServiceProfileDto profile = getRes.getBody();
        assertNotNull(profile);
        assertEquals("user@selfservice.test", profile.email());
        assertEquals("Alice Engineer", profile.name());
        assertEquals(Role.USER, profile.role());
        assertEquals("Engineering", profile.department());

        // 2. Update permitted fields (phone, location, workModel)
        UpdateSelfServiceProfileRequest updateReq = new UpdateSelfServiceProfileRequest(
                "Alice E. Smith",
                "+1999888777",
                "New York, NY",
                "Remote",
                null, null, null, null
        );

        ResponseEntity<SelfServiceProfileDto> putRes = restTemplate.exchange(
                "/api/self-service/profile",
                HttpMethod.PUT,
                new HttpEntity<>(updateReq, bearerHeaders(token)),
                SelfServiceProfileDto.class
        );

        assertEquals(HttpStatus.OK, putRes.getStatusCode());
        SelfServiceProfileDto updated = putRes.getBody();
        assertNotNull(updated);
        assertEquals("Alice E. Smith", updated.name());
        assertEquals("+1999888777", updated.phone());
        assertEquals("New York, NY", updated.location());
        assertEquals("Remote", updated.workModel());
    }

    @Test
    @DisplayName("Employee attempt to modify protected fields (role, status, email, tenantId) is rejected with 403")
    void employeeCannotModifyProtectedFields() {
        TenantRecord tenant = provisionTenant("ss-protected");
        setupUserAndEmployee(tenant, "victim@selfservice.test", Role.USER, "Bob Regular");
        String token = login(tenant, "victim@selfservice.test");

        // 1. Attempt privilege escalation: modify role to ADMIN
        UpdateSelfServiceProfileRequest escalateRole = new UpdateSelfServiceProfileRequest(
                "Bob Regular", null, null, null, "ADMIN", null, null, null
        );
        ResponseEntity<String> roleRes = restTemplate.exchange(
                "/api/self-service/profile",
                HttpMethod.PUT,
                new HttpEntity<>(escalateRole, bearerHeaders(token)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, roleRes.getStatusCode(), "Modifying role must return 403");

        // 2. Attempt email change
        UpdateSelfServiceProfileRequest changeEmail = new UpdateSelfServiceProfileRequest(
                "Bob Regular", null, null, null, null, null, "hacker@evil.com", null
        );
        ResponseEntity<String> emailRes = restTemplate.exchange(
                "/api/self-service/profile",
                HttpMethod.PUT,
                new HttpEntity<>(changeEmail, bearerHeaders(token)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, emailRes.getStatusCode(), "Modifying email must return 403");

        // 3. Attempt tenant change
        UpdateSelfServiceProfileRequest changeTenant = new UpdateSelfServiceProfileRequest(
                "Bob Regular", null, null, null, null, null, null, "other-tenant"
        );
        ResponseEntity<String> tenantRes = restTemplate.exchange(
                "/api/self-service/profile",
                HttpMethod.PUT,
                new HttpEntity<>(changeTenant, bearerHeaders(token)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, tenantRes.getStatusCode(), "Modifying tenant must return 403");
    }

    @Test
    @DisplayName("Employee can view their assigned tasks, projects, and full overview")
    void employeeAssignedTasksAndProjects() {
        TenantRecord tenant = provisionTenant("ss-tasks-projects");
        User user = setupUserAndEmployee(tenant, "worker@selfservice.test", Role.USER, "Charlie Worker");
        String token = login(tenant, "worker@selfservice.test");

        // Create Project and Task in tenant
        TenantContext.setTenant(tenant.schemaName());
        try {
            Project proj = new Project("Self Service Demo", "Description", "Client X",
                    ProjectStatus.ACTIVE, ProjectPriority.HIGH, LocalDate.now(), LocalDate.now().plusMonths(1),
                    null, user.getId(), "Charlie Worker");
            Project savedProj = projectRepository.save(proj);

            ProjectMember member = new ProjectMember(savedProj, user.getId(), "Charlie Worker", user.getEmail(), "MEMBER");
            projectMemberRepository.save(member);

            Task task = new Task(savedProj, "Implement feature XYZ", "Task details",
                    TaskStatus.IN_PROGRESS, TaskPriority.HIGH, user.getId(), "Charlie Worker",
                    user.getEmail(), LocalDate.now().plusDays(3), 8, 2, user.getId(), "Charlie Worker");
            taskRepository.save(task);
        } finally {
            TenantContext.clear();
        }

        // Test GET /api/self-service/tasks
        ResponseEntity<List<TaskResponse>> tasksRes = restTemplate.exchange(
                "/api/self-service/tasks",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, tasksRes.getStatusCode());
        assertNotNull(tasksRes.getBody());
        assertEquals(1, tasksRes.getBody().size());
        assertEquals("Implement feature XYZ", tasksRes.getBody().get(0).title());

        // Test GET /api/self-service/projects
        ResponseEntity<List<ProjectResponse>> projRes = restTemplate.exchange(
                "/api/self-service/projects",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, projRes.getStatusCode());
        assertNotNull(projRes.getBody());
        assertEquals(1, projRes.getBody().size());
        assertEquals("Self Service Demo", projRes.getBody().get(0).name());

        // Test GET /api/self-service/overview
        ResponseEntity<SelfServiceOverviewDto> overviewRes = restTemplate.exchange(
                "/api/self-service/overview",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                SelfServiceOverviewDto.class
        );
        assertEquals(HttpStatus.OK, overviewRes.getStatusCode());
        assertNotNull(overviewRes.getBody());
        assertEquals(1, overviewRes.getBody().assignedTasks().size());
        assertEquals(1, overviewRes.getBody().assignedProjects().size());
        assertNotNull(overviewRes.getBody().leaveBalances());
    }
}
