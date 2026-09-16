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

@AutoConfigureTestRestTemplate
class UserInviteFlowTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String loginAsAdmin(TenantRecord tenant) {
        TenantContext.setTenant(tenant.schemaName());
        try {
            User admin = User.newLocalUser("admin@example.test", passwordEncoder.encode("password123"), Role.ADMIN);
            userRepository.save(admin);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), "admin@example.test", "password123"), TokenResponse.class);
        return login.getBody().accessToken();
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
    void nonAdminCannotInviteUsers() {
        TenantRecord tenant = provisionTenant("invite-forbidden");

        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = User.newLocalUser("plainuser@example.test", passwordEncoder.encode("password123"), Role.USER);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> login = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), "plainuser@example.test", "password123"), TokenResponse.class);
        String userToken = login.getBody().accessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);
        HttpEntity<InviteUserRequest> inviteEntity =
                new HttpEntity<>(new InviteUserRequest("someone@example.test", Role.USER), headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users", HttpMethod.POST, inviteEntity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
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