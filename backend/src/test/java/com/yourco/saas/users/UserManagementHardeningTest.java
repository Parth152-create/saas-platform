package com.yourco.saas.users;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.RefreshRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.users.dto.ChangeRoleRequest;
import com.yourco.saas.users.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class UserManagementHardeningTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuditLogRepository auditLogRepository;

    private record TestUserSession(User user, TokenResponse tokens) {}

    private TestUserSession createAndLoginUser(TenantRecord tenant, String emailPrefix, Role role) {
        String email = emailPrefix + "_" + role.name().toLowerCase() + "@hardening.test";
        User user;
        TenantContext.setTenant(tenant.schemaName());
        try {
            user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), email, "password123"), TokenResponse.class);
        assertEquals(HttpStatus.OK, login.getStatusCode(), "Login should succeed for " + email);
        assertNotNull(login.getBody());
        return new TestUserSession(user, login.getBody());
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // =========================================================================
    // 1. Role Change Authorization & Hierarchy Tests
    // =========================================================================

    @Test
    void adminCanChangeUserToManager() {
        TenantRecord tenant = provisionTenant("role-admin-to-mgr");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession userSession = createAndLoginUser(tenant, "target", Role.USER);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/users/" + userSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(adminSession.tokens().accessToken())),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.MANAGER, response.getBody().role());

        TenantContext.setTenant(tenant.schemaName());
        try {
            User updated = userRepository.findById(userSession.user().getId()).orElseThrow();
            assertEquals(Role.MANAGER, updated.getRole());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void adminCanChangeManagerToUser() {
        TenantRecord tenant = provisionTenant("role-admin-mgr-to-usr");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession mgrSession = createAndLoginUser(tenant, "target", Role.MANAGER);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/users/" + mgrSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.USER), authHeaders(adminSession.tokens().accessToken())),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.USER, response.getBody().role());
    }

    @Test
    void adminCannotPromoteManagerToAdmin() {
        TenantRecord tenant = provisionTenant("role-admin-promote-mgr");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession mgrSession = createAndLoginUser(tenant, "target", Role.MANAGER);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + mgrSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.ADMIN), authHeaders(adminSession.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void adminCannotManageAdminUser() {
        TenantRecord tenant = provisionTenant("role-admin-target-admin");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin1", Role.ADMIN);
        TestUserSession targetAdminSession = createAndLoginUser(tenant, "admin2", Role.ADMIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + targetAdminSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(adminSession.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void superAdminCanPromoteManagerToAdminAndDemoteAdminToManager() {
        TenantRecord tenant = provisionTenant("role-superadmin-ops");
        TestUserSession superAdminSession = createAndLoginUser(tenant, "super", Role.SUPER_ADMIN);
        TestUserSession mgrSession = createAndLoginUser(tenant, "target", Role.MANAGER);

        // Promote MANAGER -> ADMIN
        ResponseEntity<UserResponse> promoteRes = restTemplate.exchange(
                "/api/users/" + mgrSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.ADMIN), authHeaders(superAdminSession.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, promoteRes.getStatusCode());
        assertEquals(Role.ADMIN, promoteRes.getBody().role());

        // Demote ADMIN -> MANAGER
        ResponseEntity<UserResponse> demoteRes = restTemplate.exchange(
                "/api/users/" + mgrSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(superAdminSession.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, demoteRes.getStatusCode());
        assertEquals(Role.MANAGER, demoteRes.getBody().role());
    }

    @Test
    void usersCannotChangeTheirOwnRole() {
        TenantRecord tenant = provisionTenant("role-self-change");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + adminSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(adminSession.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void superAdminRoleCannotBeAssignedThroughUserManagement() {
        TenantRecord tenant = provisionTenant("role-no-superadmin-assign");
        TestUserSession superAdminSession = createAndLoginUser(tenant, "super", Role.SUPER_ADMIN);
        TestUserSession userSession = createAndLoginUser(tenant, "user", Role.USER);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + userSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.SUPER_ADMIN), authHeaders(superAdminSession.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void managerAndUserCannotManageRoles() {
        TenantRecord tenant = provisionTenant("role-unauth-roles");
        TestUserSession mgrSession = createAndLoginUser(tenant, "mgr", Role.MANAGER);
        TestUserSession userSession = createAndLoginUser(tenant, "usr", Role.USER);
        TestUserSession targetSession = createAndLoginUser(tenant, "target", Role.USER);

        // MANAGER attempt
        ResponseEntity<String> mgrRes = restTemplate.exchange(
                "/api/users/" + targetSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(mgrSession.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, mgrRes.getStatusCode());

        // USER attempt
        ResponseEntity<String> userRes = restTemplate.exchange(
                "/api/users/" + targetSession.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(userSession.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userRes.getStatusCode());
    }

    // =========================================================================
    // 2. User Deactivation Tests
    // =========================================================================

    @Test
    void authorizedAdminCanDeactivateUser() {
        TenantRecord tenant = provisionTenant("deact-admin");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession targetSession = createAndLoginUser(tenant, "target", Role.USER);

        ResponseEntity<UserResponse> response = restTemplate.exchange(
                "/api/users/" + targetSession.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminSession.tokens().accessToken())),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(UserStatus.DISABLED, response.getBody().status());

        TenantContext.setTenant(tenant.schemaName());
        try {
            User disabledUser = userRepository.findById(targetSession.user().getId()).orElseThrow();
            assertEquals(UserStatus.DISABLED, disabledUser.getStatus());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void adminCannotDeactivateAdminUser_superAdminCanDeactivateAdmin() {
        TenantRecord tenant = provisionTenant("deact-admin-matrix");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin1", Role.ADMIN);
        TestUserSession targetAdmin = createAndLoginUser(tenant, "admin2", Role.ADMIN);
        TestUserSession superAdmin = createAndLoginUser(tenant, "super", Role.SUPER_ADMIN);

        // ADMIN cannot deactivate ADMIN
        ResponseEntity<String> adminFailRes = restTemplate.exchange(
                "/api/users/" + targetAdmin.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminSession.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, adminFailRes.getStatusCode());

        // SUPER_ADMIN can deactivate ADMIN
        ResponseEntity<UserResponse> superSuccessRes = restTemplate.exchange(
                "/api/users/" + targetAdmin.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(superAdmin.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, superSuccessRes.getStatusCode());
        assertEquals(UserStatus.DISABLED, superSuccessRes.getBody().status());
    }

    @Test
    void userCannotDeactivateSelf() {
        TenantRecord tenant = provisionTenant("deact-self");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + adminSession.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminSession.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void superAdminCannotBeDeactivated() {
        TenantRecord tenant = provisionTenant("deact-superadmin-guard");
        TestUserSession superAdmin1 = createAndLoginUser(tenant, "super1", Role.SUPER_ADMIN);
        TestUserSession superAdmin2 = createAndLoginUser(tenant, "super2", Role.SUPER_ADMIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/" + superAdmin2.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(superAdmin1.tokens().accessToken())),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // =========================================================================
    // 3. Post-Deactivation Authentication & Session Invalidation Tests
    // =========================================================================

    @Test
    void deactivatedUserCannotAuthenticateOrRefreshSession() {
        TenantRecord tenant = provisionTenant("deact-auth-blocked");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession targetSession = createAndLoginUser(tenant, "target", Role.USER);

        String targetEmail = targetSession.user().getEmail();
        String activeRefreshToken = targetSession.tokens().refreshToken();
        String activeAccessToken = targetSession.tokens().accessToken();

        // Deactivate the user
        ResponseEntity<UserResponse> deactRes = restTemplate.exchange(
                "/api/users/" + targetSession.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminSession.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, deactRes.getStatusCode());

        // 1. Password login must be rejected
        ResponseEntity<String> loginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), targetEmail, "password123"),
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, loginRes.getStatusCode());

        // 2. Refresh token must be rejected
        ResponseEntity<String> refreshRes = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(activeRefreshToken),
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, refreshRes.getStatusCode());

        // 3. Accessing tenant resources with existing access token must be rejected by JwtAuthenticationFilter
        ResponseEntity<String> apiRes = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(activeAccessToken)),
                String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, apiRes.getStatusCode());
    }

    @Test
    void deactivatedUserHistoricalRecordsRemainIntact() {
        TenantRecord tenant = provisionTenant("deact-history-intact");
        TestUserSession adminSession = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession targetSession = createAndLoginUser(tenant, "target", Role.USER);

        // Deactivate
        restTemplate.exchange(
                "/api/users/" + targetSession.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminSession.tokens().accessToken())),
                UserResponse.class
        );

        // Verify user row is NOT deleted from database, only status is DISABLED
        TenantContext.setTenant(tenant.schemaName());
        try {
            User userInDb = userRepository.findById(targetSession.user().getId()).orElse(null);
            assertNotNull(userInDb, "Historical user entity must not be deleted");
            assertEquals(UserStatus.DISABLED, userInDb.getStatus());
            assertEquals(targetSession.user().getEmail(), userInDb.getEmail());
            assertNotNull(userInDb.getCreatedAt());
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // 4. Tenant Isolation Tests
    // =========================================================================

    @Test
    void crossTenantRoleChangeAndDeactivationRejected() {
        TenantRecord tenantA = provisionTenant("tenant-a-iso");
        TenantRecord tenantB = provisionTenant("tenant-b-iso");

        TestUserSession adminA = createAndLoginUser(tenantA, "adminA", Role.ADMIN);
        TestUserSession userB = createAndLoginUser(tenantB, "userB", Role.USER);

        // Admin A tries to change Tenant B user's role
        ResponseEntity<String> roleChangeRes = restTemplate.exchange(
                "/api/users/" + userB.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(adminA.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, roleChangeRes.getStatusCode());

        // Admin A tries to deactivate Tenant B user
        ResponseEntity<String> deactRes = restTemplate.exchange(
                "/api/users/" + userB.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminA.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, deactRes.getStatusCode());

        // Verify Tenant B user is completely unaffected
        TenantContext.setTenant(tenantB.schemaName());
        try {
            User userInB = userRepository.findById(userB.user().getId()).orElseThrow();
            assertEquals(Role.USER, userInB.getRole(), "User B role must remain unchanged");
            assertEquals(UserStatus.ACTIVE, userInB.getStatus(), "User B status must remain ACTIVE");
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // 5. Audit Logging Tests
    // =========================================================================

    @Test
    void roleChangeAndDeactivationProduceAuditEvents() {
        TenantRecord tenant = provisionTenant("audit-user-mgmt");
        TestUserSession admin = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession target = createAndLoginUser(tenant, "target", Role.USER);

        // 1. Role Change
        restTemplate.exchange(
                "/api/users/" + target.user().getId() + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), authHeaders(admin.tokens().accessToken())),
                UserResponse.class
        );

        // 2. Deactivation
        restTemplate.exchange(
                "/api/users/" + target.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(admin.tokens().accessToken())),
                UserResponse.class
        );

        // Verify audit logs in tenant schema
        TenantContext.setTenant(tenant.schemaName());
        try {
            List<AuditLog> logs = auditLogRepository.findAll();

            boolean foundRoleChange = logs.stream().anyMatch(l ->
                    "USER_ROLE_CHANGE".equals(l.getAction()) &&
                    "SUCCESS".equals(l.getOutcome()) &&
                    admin.user().getId().equals(l.getActorId()) &&
                    l.getDetails().contains("previous_role=USER") &&
                    l.getDetails().contains("new_role=MANAGER")
            );
            assertTrue(foundRoleChange, "Audit log for USER_ROLE_CHANGE must be recorded with actor and role details");

            boolean foundDeactivation = logs.stream().anyMatch(l ->
                    "USER_DEACTIVATE".equals(l.getAction()) &&
                    "SUCCESS".equals(l.getOutcome()) &&
                    admin.user().getId().equals(l.getActorId()) &&
                    l.getDetails().contains("target_user=" + target.user().getId())
            );
            assertTrue(foundDeactivation, "Audit log for USER_DEACTIVATE must be recorded with actor details");
        } finally {
            TenantContext.clear();
        }
    }

    // =========================================================================
    // 5. User Reactivation Tests
    // =========================================================================

    @Test
    void adminCanReactivateDeactivatedUserAndAuditLogRecorded() {
        TenantRecord tenant = provisionTenant("reactivate-success");
        TestUserSession admin = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession target = createAndLoginUser(tenant, "target", Role.USER);

        // Deactivate first
        ResponseEntity<UserResponse> deactRes = restTemplate.exchange(
                "/api/users/" + target.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(admin.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, deactRes.getStatusCode());
        assertEquals(UserStatus.DISABLED, deactRes.getBody().status());

        // Now reactivate
        ResponseEntity<UserResponse> reactRes = restTemplate.exchange(
                "/api/users/" + target.user().getId() + "/reactivate",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(admin.tokens().accessToken())),
                UserResponse.class
        );
        assertEquals(HttpStatus.OK, reactRes.getStatusCode());
        assertNotNull(reactRes.getBody());
        assertEquals(UserStatus.ACTIVE, reactRes.getBody().status());

        // Verify status in DB
        TenantContext.setTenant(tenant.schemaName());
        try {
            User restored = userRepository.findById(target.user().getId()).orElseThrow();
            assertEquals(UserStatus.ACTIVE, restored.getStatus());

            // Verify audit log
            List<AuditLog> logs = auditLogRepository.findAll();
            boolean foundReactivate = logs.stream().anyMatch(l ->
                    "USER_REACTIVATE".equals(l.getAction()) &&
                    "SUCCESS".equals(l.getOutcome()) &&
                    admin.user().getId().equals(l.getActorId()) &&
                    l.getDetails().contains("target_user=" + target.user().getId())
            );
            assertTrue(foundReactivate, "Audit log for USER_REACTIVATE must be recorded with actor details");
        } finally {
            TenantContext.clear();
        }

        // Verify reactivated user can login successfully
        ResponseEntity<TokenResponse> loginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), target.user().getEmail(), "password123"),
                TokenResponse.class
        );
        assertEquals(HttpStatus.OK, loginRes.getStatusCode());
        assertNotNull(loginRes.getBody().accessToken());
    }

    @Test
    void regularUserAndManagerCannotReactivateUser() {
        TenantRecord tenant = provisionTenant("reactivate-rbac");
        TestUserSession admin = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession manager = createAndLoginUser(tenant, "manager", Role.MANAGER);
        TestUserSession user = createAndLoginUser(tenant, "user", Role.USER);
        TestUserSession target = createAndLoginUser(tenant, "target", Role.USER);

        // Deactivate via admin
        restTemplate.exchange(
                "/api/users/" + target.user().getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(admin.tokens().accessToken())),
                UserResponse.class
        );

        // USER role cannot reactivate (403)
        ResponseEntity<String> userAttempt = restTemplate.exchange(
                "/api/users/" + target.user().getId() + "/reactivate",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(user.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, userAttempt.getStatusCode());

        // MANAGER role cannot reactivate (403)
        ResponseEntity<String> managerAttempt = restTemplate.exchange(
                "/api/users/" + target.user().getId() + "/reactivate",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(manager.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, managerAttempt.getStatusCode());
    }

    @Test
    void cannotReactivateActiveUser() {
        TenantRecord tenant = provisionTenant("reactivate-active");
        TestUserSession admin = createAndLoginUser(tenant, "admin", Role.ADMIN);
        TestUserSession target = createAndLoginUser(tenant, "target", Role.USER);

        // Target is already active
        ResponseEntity<String> res = restTemplate.exchange(
                "/api/users/" + target.user().getId() + "/reactivate",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(admin.tokens().accessToken())),
                String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }
}

