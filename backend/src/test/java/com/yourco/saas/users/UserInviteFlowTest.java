package com.yourco.saas.users;

import com.yourco.saas.auth.dto.AcceptInviteRequest;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.users.dto.InviteUserRequest;
import com.yourco.saas.users.dto.InviteUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
class UserInviteFlowTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String loginAsRole(TenantRecord tenant, Role role) {
        String email = role.name().toLowerCase() + "@example.test";
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = User.newLocalUser(email, passwordEncoder.encode("password123"), role);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), email, "password123"), TokenResponse.class);
        assertNotNull(login.getBody(), "Login failed for role " + role);
        return login.getBody().accessToken();
    }

    private String loginAsAdmin(TenantRecord tenant) {
        return loginAsRole(tenant, Role.ADMIN);
    }

    @Test
    void adminInvitesUserAndUserAcceptsWithPassword() {
        TenantRecord tenant = provisionTenant("invite-accept");
        String adminToken = loginAsAdmin(tenant);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<InviteUserRequest> inviteEntity =
                new HttpEntity<>(new InviteUserRequest("teammate@example.test", Role.USER), headers);

        ResponseEntity<InviteUserResponse> inviteResponse = restTemplate.exchange(
                "/api/users", HttpMethod.POST, inviteEntity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, inviteResponse.getStatusCode());
        assertNotNull(inviteResponse.getBody());
        String token = inviteResponse.getBody().inviteToken();
        assertNotNull(token);

        ResponseEntity<TokenResponse> acceptResponse = restTemplate.postForEntity(
                "/api/auth/accept-invite",
                new AcceptInviteRequest(tenant.tenantId(), token, "teammatepassword"),
                TokenResponse.class);

        assertEquals(HttpStatus.OK, acceptResponse.getStatusCode());
        assertNotNull(acceptResponse.getBody().accessToken());

        TenantContext.setTenant(tenant.schemaName());
        try {
            User teammate = userRepository.findByEmailIgnoreCase("teammate@example.test").orElseThrow();
            assertEquals(UserStatus.ACTIVE, teammate.getStatus());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void superAdminCanInviteSuperAdmin() {
        TenantRecord tenant = provisionTenant("sa-inv-sa");
        String token = loginAsRole(tenant, Role.SUPER_ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("new-sa@example.test", Role.SUPER_ADMIN), headers);

        ResponseEntity<InviteUserResponse> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-sa@example.test", response.getBody().email());
        assertEquals(Role.SUPER_ADMIN, response.getBody().role());
    }

    @Test
    void superAdminCanInviteAdmin() {
        TenantRecord tenant = provisionTenant("sa-inv-admin");
        String token = loginAsRole(tenant, Role.SUPER_ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("new-admin@example.test", Role.ADMIN), headers);

        ResponseEntity<InviteUserResponse> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.ADMIN, response.getBody().role());
    }

    @Test
    void adminCanInviteAdmin() {
        TenantRecord tenant = provisionTenant("admin-inv-admin");
        String token = loginAsRole(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("peer-admin@example.test", Role.ADMIN), headers);

        ResponseEntity<InviteUserResponse> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.ADMIN, response.getBody().role());
    }

    @Test
    void adminCanInviteManager() {
        TenantRecord tenant = provisionTenant("admin-inv-mgr");
        String token = loginAsRole(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("mgr@example.test", Role.MANAGER), headers);

        ResponseEntity<InviteUserResponse> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.MANAGER, response.getBody().role());
    }

    @Test
    void adminCanInviteUser() {
        TenantRecord tenant = provisionTenant("admin-inv-user");
        String token = loginAsRole(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("usr@example.test", Role.USER), headers);

        ResponseEntity<InviteUserResponse> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, InviteUserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Role.USER, response.getBody().role());
    }

    @Test
    void adminAttemptingToInviteSuperAdminReceivesForbidden() {
        TenantRecord tenant = provisionTenant("admin-inv-sa-fail");
        String token = loginAsRole(tenant, Role.ADMIN);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<InviteUserRequest> entity =
                new HttpEntity<>(new InviteUserRequest("escalate@example.test", Role.SUPER_ADMIN), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        TenantContext.setTenant(tenant.schemaName());
        try {
            assertTrue(userRepository.findByEmailIgnoreCase("escalate@example.test").isEmpty(),
                    "Super admin user must not have been created or saved");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void nonAdminCannotInviteUsers() {
        TenantRecord tenant = provisionTenant("invite-forbidden");
        String userToken = loginAsRole(tenant, Role.USER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<InviteUserRequest> inviteEntity =
                new HttpEntity<>(new InviteUserRequest("someone@example.test", Role.USER), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, inviteEntity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void managerAttemptingToInviteAnyRoleRemainsForbidden() {
        TenantRecord tenant = provisionTenant("mgr-inv-fail");
        String token = loginAsRole(tenant, Role.MANAGER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        for (Role targetRole : Role.values()) {
            HttpEntity<InviteUserRequest> entity =
                    new HttpEntity<>(new InviteUserRequest("mgr-" + targetRole.name().toLowerCase() + "@example.test", targetRole), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/users", HttpMethod.POST, entity, String.class);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "Manager should not invite " + targetRole);
        }
    }

    @Test
    void userAttemptingToInviteAnyRoleRemainsForbidden() {
        TenantRecord tenant = provisionTenant("user-inv-fail");
        String token = loginAsRole(tenant, Role.USER);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        for (Role targetRole : Role.values()) {
            HttpEntity<InviteUserRequest> entity =
                    new HttpEntity<>(new InviteUserRequest("user-" + targetRole.name().toLowerCase() + "@example.test", targetRole), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/users", HttpMethod.POST, entity, String.class);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), "User should not invite " + targetRole);
        }
    }

    @Test
    void expiredInviteTokenIsRejected() {
        TenantRecord tenant = provisionTenant("invite-expired");

        TenantContext.setTenant(tenant.schemaName());
        try {
            User expiredInvite = User.newInvitedUser(
                    "late@example.test", Role.USER, "expired-token-123", Instant.now().minus(1, ChronoUnit.DAYS));
            userRepository.save(expiredInvite);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/accept-invite",
                new AcceptInviteRequest(tenant.tenantId(), "expired-token-123", "somepassword"),
                String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}