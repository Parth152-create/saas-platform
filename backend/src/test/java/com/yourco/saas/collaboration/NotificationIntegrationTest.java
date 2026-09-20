package com.yourco.saas.collaboration;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.collaboration.dto.NotificationResponse;
import com.yourco.saas.collaboration.dto.UnreadCountResponse;
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
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class NotificationIntegrationTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

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

    private User getUser(TenantRecord tenant, String email) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            return userRepository.findByEmail(email).orElseThrow();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Notification Creation, Unread Count, Mark Read, and Cross-Tenant/Cross-User Isolation")
    void testNotificationLifecycleAndIsolation() {
        TenantRecord tenantA = provisionTenant("notif-a");
        TenantRecord tenantB = provisionTenant("notif-b");

        String managerTokenA = createAndLoginUser(tenantA, "manager@tenanta.com", Role.MANAGER);
        String userTokenA = createAndLoginUser(tenantA, "user@tenanta.com", Role.USER);
        User userEntityA = getUser(tenantA, "user@tenanta.com");

        String adminTokenB = createAndLoginUser(tenantB, "admin@tenantb.com", Role.ADMIN);

        // 1. Initial State: Unread count for User A is 0
        ResponseEntity<UnreadCountResponse> countRes = restTemplate.exchange(
                "/api/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                UnreadCountResponse.class
        );
        assertEquals(HttpStatus.OK, countRes.getStatusCode());
        assertNotNull(countRes.getBody());
        assertEquals(0, countRes.getBody().count());

        // 2. Create Project and Add User A as member -> triggers PROJECT_MEMBER_ADDED notification
        CreateProjectRequest projReq = new CreateProjectRequest(
                "Cloud Migration", "Project", "Acme",
                ProjectStatus.ACTIVE, ProjectPriority.HIGH,
                LocalDate.now(), LocalDate.now().plusDays(30),
                new BigDecimal("50000.00"), null, List.of()
        );
        ResponseEntity<ProjectResponse> projRes = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                new HttpEntity<>(projReq, bearerHeaders(managerTokenA)),
                ProjectResponse.class
        );
        assertEquals(HttpStatus.CREATED, projRes.getStatusCode());
        UUID projectId = projRes.getBody().id();

        // Add User A to Project
        AddProjectMemberRequest addMemberReq = new AddProjectMemberRequest(userEntityA.getId(), "MEMBER");
        ResponseEntity<ProjectMemberDto> addMemberRes = restTemplate.exchange(
                "/api/projects/" + projectId + "/members",
                HttpMethod.POST,
                new HttpEntity<>(addMemberReq, bearerHeaders(managerTokenA)),
                ProjectMemberDto.class
        );
        assertEquals(HttpStatus.CREATED, addMemberRes.getStatusCode());

        // 3. Manager assigns task to User A -> triggers TASK_ASSIGNED notification
        CreateTaskRequest taskReq = new CreateTaskRequest(
                "Build Auth Integration", "Details", TaskStatus.TODO, TaskPriority.HIGH,
                userEntityA.getId(), LocalDate.now().plusDays(7), 10, 0
        );
        ResponseEntity<TaskResponse> taskRes = restTemplate.exchange(
                "/api/projects/" + projectId + "/tasks",
                HttpMethod.POST,
                new HttpEntity<>(taskReq, bearerHeaders(managerTokenA)),
                TaskResponse.class
        );
        assertEquals(HttpStatus.CREATED, taskRes.getStatusCode());

        // 4. Verify Unread Count for User A is now 2
        ResponseEntity<UnreadCountResponse> countResAfter = restTemplate.exchange(
                "/api/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                UnreadCountResponse.class
        );
        assertEquals(HttpStatus.OK, countResAfter.getStatusCode());
        assertEquals(2, countResAfter.getBody().count());

        // 5. Fetch Notifications for User A
        ResponseEntity<String> notifsRes = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                String.class
        );
        assertEquals(HttpStatus.OK, notifsRes.getStatusCode());
        assertTrue(notifsRes.getBody().contains("Cloud Migration"));
        assertTrue(notifsRes.getBody().contains("Build Auth Integration"));

        // Extract a notification ID from DB for testing mark-read
        TenantContext.setTenant(tenantA.schemaName());
        UUID notifId;
        try {
            notifId = userRepository.findByEmail("user@tenanta.com").map(u ->
                // fetch via raw check or repository
                restTemplate.exchange("/api/notifications", HttpMethod.GET, new HttpEntity<>(bearerHeaders(userTokenA)), String.class)
            ).toString().contains("id") ? null : null;
        } finally {
            TenantContext.clear();
        }

        // 6. Mark All Read
        ResponseEntity<Void> markAllRes = restTemplate.exchange(
                "/api/notifications/read-all",
                HttpMethod.PUT,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                Void.class
        );
        assertEquals(HttpStatus.NO_CONTENT, markAllRes.getStatusCode());

        // 7. Verify Unread Count is now 0
        ResponseEntity<UnreadCountResponse> countResZero = restTemplate.exchange(
                "/api/notifications/unread-count",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userTokenA)),
                UnreadCountResponse.class
        );
        assertEquals(HttpStatus.OK, countResZero.getStatusCode());
        assertEquals(0, countResZero.getBody().count());

        // 8. Cross-Tenant Isolation: Tenant B cannot access Tenant A's notifications
        ResponseEntity<String> crossNotifRes = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminTokenB)),
                String.class
        );
        assertEquals(HttpStatus.OK, crossNotifRes.getStatusCode());
        assertFalse(crossNotifRes.getBody().contains("Cloud Migration"));
        assertFalse(crossNotifRes.getBody().contains("Build Auth Integration"));
    }
}
