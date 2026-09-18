package com.yourco.saas.users;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.domain.user.UserTestFactory;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.users.dto.InviteUserRequest;
import com.yourco.saas.users.dto.InviteUserResponse;
import com.yourco.saas.users.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class UserControllerTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String loginAs(TenantRecord tenant, Role role) {
        String email = role.name().toLowerCase() + "@users.test";
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser(email, role);
            user.setPasswordHash(passwordEncoder.encode("password123"));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(tenant.tenantId(), email, "password123"), TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        return loginResponse.getBody().accessToken();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void adminCanListAndInviteUsers() {
        TenantRecord tenant = provisionTenant("user-flow");
        String adminToken = loginAs(tenant, Role.ADMIN);

        // List initial users (should contain the admin)
        ResponseEntity<UserResponse[]> listResponse = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                UserResponse[].class
        );
        assertEquals(HttpStatus.OK, listResponse.getStatusCode());
        assertNotNull(listResponse.getBody());
        assertTrue(listResponse.getBody().length >= 1);

        // Invite a new member
        InviteUserRequest req = new InviteUserRequest("new.member@company.com", Role.USER);
        ResponseEntity<InviteUserResponse> inviteResponse = restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(req, authHeaders(adminToken)),
                InviteUserResponse.class
        );
        assertEquals(HttpStatus.CREATED, inviteResponse.getStatusCode());
        assertNotNull(inviteResponse.getBody());
        assertEquals("new.member@company.com", inviteResponse.getBody().email());
        assertNotNull(inviteResponse.getBody().inviteToken());

        // Re-fetch user list and verify invited user is present with status INVITED
        ResponseEntity<UserResponse[]> updatedListResponse = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                UserResponse[].class
        );
        assertEquals(HttpStatus.OK, updatedListResponse.getStatusCode());
        assertNotNull(updatedListResponse.getBody());
        boolean foundInvited = false;
        for (UserResponse u : updatedListResponse.getBody()) {
            if ("new.member@company.com".equals(u.email())) {
                foundInvited = true;
                assertEquals(UserStatus.INVITED, u.status());
                assertNotNull(u.inviteToken());
            }
        }
        assertTrue(foundInvited, "Invited user should be returned in user list");
    }

    @Test
    void nonAdminCannotListUsers() {
        TenantRecord tenant = provisionTenant("user-nonadmin");
        String userToken = loginAs(tenant, Role.USER);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                String.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
