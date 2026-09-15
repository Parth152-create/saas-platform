package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.SignupRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.integration.IntegrationTestBase;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
class SignupFlowTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TenantRegistryService tenantRegistryService;

    @Autowired
    UserRepository userRepository;

    @Test
    void signupCreatesTenantAndSuperAdminAndAllowsLogin() {
        String tenantId = "signup-test-" + System.nanoTime();

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantId, "owner@example.test", "password123"),
                TokenResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());

        Optional<TenantRecord> tenant = tenantRegistryService.findByTenantId(tenantId);
        assertTrue(tenant.isPresent());
        assertEquals("FREE", tenant.get().plan());
        assertEquals("ACTIVE", tenant.get().status());
        trackProvisionedSchema(tenant.get().schemaName());

        TenantContext.setTenant(tenant.get().schemaName());
        try {
            User user = userRepository.findByEmailIgnoreCase("owner@example.test").orElseThrow();
            assertEquals(Role.SUPER_ADMIN, user.getRole());
            assertEquals(UserStatus.ACTIVE, user.getStatus());
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantId, "owner@example.test", "password123"),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
    }

    @Test
    void duplicateTenantIdIsRejected() {
        String tenantId = "signup-dup-" + System.nanoTime();

        ResponseEntity<TokenResponse> first = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantId, "first@example.test", "password123"),
                TokenResponse.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        tenantRegistryService.findByTenantId(tenantId)
                .ifPresent(t -> trackProvisionedSchema(t.schemaName()));

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantId, "second@example.test", "password123"),
                String.class);
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }

    @Test
    void tooShortPasswordIsRejected() {
        String tenantId = "signup-badpw-" + System.nanoTime();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantId, "owner@example.test", "short"),
                String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}