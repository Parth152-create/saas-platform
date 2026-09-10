package com.yourco.saas.rbac;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Codifies the RBAC hierarchy verification that was previously only
 * hand-checked with curl for a single ADMIN user ("Verified 200/200/200/403
 * across user/manager/admin/super-admin for an ADMIN test user"). This runs
 * the full matrix for all four roles against the real RbacDebugController
 * endpoints, using REAL JWTs from a real /api/auth/login call - not
 * fabricated tokens - so it exercises the actual JwtAuthenticationFilter ->
 * RoleHierarchy -> MethodSecurityExpressionHandler -> @PreAuthorize chain,
 * not just the hierarchy logic in isolation.
 * <p>
 * Hierarchy under test: SUPER_ADMIN > ADMIN > MANAGER > USER.
 * <p>
 * Each @Test provisions its own tenant/user rather than sharing a fixture
 * across the 4 role tests - same reasoning as CrossTenantIsolationTest:
 * fully independent, order-immune, at the cost of a little redundant setup.
 */
@AutoConfigureTestRestTemplate
class RbacHierarchyTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    private String loginAs(TenantRecord tenant, Role role) {
        String email = role.name().toLowerCase() + "@rbac.test";

        TenantContext.setTenant(tenant.schemaName());
        User user = UserTestFactory.localUser(email, role);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        TenantContext.clear();

        TokenResponse tokens = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), email, RAW_PASSWORD),
                TokenResponse.class).getBody();
        assertNotNull(tokens, "Login should have succeeded for role " + role);
        return tokens.accessToken();
    }

    private HttpStatus callDebugEndpoint(String path, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> response =
                restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return (HttpStatus) response.getStatusCode();
    }

    private void assertHierarchyRow(Role role, HttpStatus userStatus, HttpStatus managerStatus,
                                     HttpStatus adminStatus, HttpStatus superAdminStatus) {
        TenantRecord tenant = provisionTenant("rbac-" + role.name().toLowerCase());
        String token = loginAs(tenant, role);

        assertEquals(userStatus, callDebugEndpoint("/api/rbac-debug/user", token), role + " on /user");
        assertEquals(managerStatus, callDebugEndpoint("/api/rbac-debug/manager", token), role + " on /manager");
        assertEquals(adminStatus, callDebugEndpoint("/api/rbac-debug/admin", token), role + " on /admin");
        assertEquals(superAdminStatus, callDebugEndpoint("/api/rbac-debug/super-admin", token), role + " on /super-admin");
    }

    @Test
    void userRoleOnlyReachesUserEndpoint() {
        assertHierarchyRow(Role.USER, HttpStatus.OK, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    @Test
    void managerRoleReachesUserAndManagerEndpoints() {
        assertHierarchyRow(Role.MANAGER, HttpStatus.OK, HttpStatus.OK, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN);
    }

    @Test
    void adminRoleReachesEverythingExceptSuperAdmin() {
        assertHierarchyRow(Role.ADMIN, HttpStatus.OK, HttpStatus.OK, HttpStatus.OK, HttpStatus.FORBIDDEN);
    }

    @Test
    void superAdminRoleReachesEverything() {
        assertHierarchyRow(Role.SUPER_ADMIN, HttpStatus.OK, HttpStatus.OK, HttpStatus.OK, HttpStatus.OK);
    }

    @Test
    void missingAuthorizationHeaderReturns401NotForbidden() {
        provisionTenant("rbac-noauth");
        ResponseEntity<String> response = restTemplate.getForEntity("/api/rbac-debug/user", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "No token at all should hit the authenticationEntryPoint (401), not the accessDeniedHandler (403)");
    }
}