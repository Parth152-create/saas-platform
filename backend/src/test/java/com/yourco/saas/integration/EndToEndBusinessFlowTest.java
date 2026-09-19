package com.yourco.saas.integration;

import com.stripe.model.checkout.Session;
import com.yourco.saas.auth.dto.AcceptInviteRequest;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.LogoutRequest;
import com.yourco.saas.auth.dto.RefreshRequest;
import com.yourco.saas.auth.dto.SignupRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.billing.StripeCheckoutSessionCreator;
import com.yourco.saas.billing.dto.BillingSummaryResponse;
import com.yourco.saas.billing.dto.CreateCheckoutSessionRequest;
import com.yourco.saas.billing.dto.CreateCheckoutSessionResponse;
import com.yourco.saas.billing.dto.FeatureEntitlementsResponse;
import com.yourco.saas.common.exception.ErrorResponse;
import com.yourco.saas.domain.billing.PlanTier;
import com.yourco.saas.domain.hrm.EmployeeStatus;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.hrm.dto.CreateDepartmentRequest;
import com.yourco.saas.hrm.dto.CreateEmployeeRequest;
import com.yourco.saas.hrm.dto.DepartmentDto;
import com.yourco.saas.hrm.dto.EmployeeDto;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.users.dto.ChangeRoleRequest;
import com.yourco.saas.users.dto.InviteUserRequest;
import com.yourco.saas.users.dto.InviteUserResponse;
import com.yourco.saas.users.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-End Business Flow Test:
 * Executes the complete realistic 20-step organization lifecycle against
 * the real HTTP filter chain, multi-tenant Postgres schema isolation,
 * Redis session store, and Spring Security RBAC hierarchy.
 */
@AutoConfigureTestRestTemplate
class EndToEndBusinessFlowTest extends IntegrationTestBase {

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
    @DisplayName("Complete 20-Step Realistic Organization Workflow")
    void executeCompleteOrganizationWorkflow() throws Exception {
        String uniqueSuffix = Long.toHexString(System.nanoTime());
        String tenantId = "acme-" + uniqueSuffix;
        String ownerEmail = "owner-" + uniqueSuffix + "@acme.test";
        String ownerPassword = "OwnerPassword123!";
        String teammateEmail = "teammate-" + uniqueSuffix + "@acme.test";
        String teammatePassword = "TeammatePassword123!";

        // ---------------------------------------------------------------------
        // Step 1: Create/register a new tenant
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> signupRes = restTemplate.postForEntity(
                "/api/auth/signup",
                new SignupRequest(tenantId, ownerEmail, ownerPassword),
                TokenResponse.class);
        assertEquals(HttpStatus.CREATED, signupRes.getStatusCode(), "Step 1: Tenant signup should return 201");
        assertNotNull(signupRes.getBody());
        assertNotNull(signupRes.getBody().accessToken());

        TenantRecord tenantA = tenantRegistryService.findByTenantId(tenantId).orElseThrow();
        trackProvisionedSchema(tenantA.schemaName());
        assertEquals("FREE", tenantA.plan());
        assertEquals("ACTIVE", tenantA.status());

        // ---------------------------------------------------------------------
        // Step 2: Log in as the initial administrator
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> adminLoginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantId, ownerEmail, ownerPassword),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, adminLoginRes.getStatusCode(), "Step 2: Admin login should return 200");
        TokenResponse adminTokens = adminLoginRes.getBody();
        assertNotNull(adminTokens);
        String adminAccessToken = adminTokens.accessToken();

        // ---------------------------------------------------------------------
        // Step 3: Create/invite another user (USER role)
        // ---------------------------------------------------------------------
        ResponseEntity<InviteUserResponse> inviteRes = restTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(new InviteUserRequest(teammateEmail, Role.USER), bearerHeaders(adminAccessToken)),
                InviteUserResponse.class);
        assertEquals(HttpStatus.CREATED, inviteRes.getStatusCode(), "Step 3: User invitation should return 201");
        InviteUserResponse inviteBody = inviteRes.getBody();
        assertNotNull(inviteBody);
        String inviteToken = inviteBody.inviteToken();
        assertNotNull(inviteToken);

        // ---------------------------------------------------------------------
        // Step 4: Accept the invitation
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> acceptRes = restTemplate.postForEntity(
                "/api/auth/accept-invite",
                new AcceptInviteRequest(tenantId, inviteToken, teammatePassword),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, acceptRes.getStatusCode(), "Step 4: Accept invite should return 200");
        TokenResponse initialTeammateTokens = acceptRes.getBody();
        assertNotNull(initialTeammateTokens);

        // ---------------------------------------------------------------------
        // Step 5: Verify the new user's role
        // ---------------------------------------------------------------------
        ResponseEntity<List<UserResponse>> usersListRes = restTemplate.exchange(
                "/api/users",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminAccessToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, usersListRes.getStatusCode(), "Step 5: Admin can list workspace users");
        List<UserResponse> usersList = usersListRes.getBody();
        assertNotNull(usersList);
        UserResponse teammateUser = usersList.stream()
                .filter(u -> teammateEmail.equalsIgnoreCase(u.email()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Invited user must appear in tenant user list"));
        assertEquals(Role.USER, teammateUser.role(), "Step 5: New user role should be USER");
        assertEquals(UserStatus.ACTIVE, teammateUser.status(), "Step 5: New user status should be ACTIVE");
        UUID teammateUserId = teammateUser.id();

        // ---------------------------------------------------------------------
        // Step 6: Change the user's role to MANAGER
        // ---------------------------------------------------------------------
        ResponseEntity<UserResponse> roleChangeRes = restTemplate.exchange(
                "/api/users/" + teammateUserId + "/role",
                HttpMethod.PATCH,
                new HttpEntity<>(new ChangeRoleRequest(Role.MANAGER), bearerHeaders(adminAccessToken)),
                UserResponse.class);
        assertEquals(HttpStatus.OK, roleChangeRes.getStatusCode(), "Step 6: Role change should return 200");
        assertEquals(Role.MANAGER, roleChangeRes.getBody().role(), "Step 6: User role should now be MANAGER");

        // ---------------------------------------------------------------------
        // Step 7: Log in/refresh as the changed user
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> teammateLoginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantId, teammateEmail, teammatePassword),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, teammateLoginRes.getStatusCode(), "Step 7: Login as updated user should return 200");
        TokenResponse managerTokens = teammateLoginRes.getBody();
        assertNotNull(managerTokens);
        String managerAccessToken = managerTokens.accessToken();

        // ---------------------------------------------------------------------
        // Step 8: Verify permissions changed correctly
        // ---------------------------------------------------------------------
        ResponseEntity<String> mgrAccessRes = restTemplate.exchange(
                "/api/rbac-debug/manager",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(managerAccessToken)),
                String.class);
        assertEquals(HttpStatus.OK, mgrAccessRes.getStatusCode(), "Step 8: MANAGER must access manager endpoint");

        ResponseEntity<String> adminForbiddenRes = restTemplate.exchange(
                "/api/rbac-debug/admin",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(managerAccessToken)),
                String.class);
        assertEquals(HttpStatus.FORBIDDEN, adminForbiddenRes.getStatusCode(), "Step 8: MANAGER must be forbidden from admin endpoint");

        // ---------------------------------------------------------------------
        // Step 9: Create HRM data (Department & Employee)
        // ---------------------------------------------------------------------
        CreateDepartmentRequest deptReq = new CreateDepartmentRequest("Product Platform", "Jane Lead", new BigDecimal("75.00"));
        ResponseEntity<DepartmentDto> deptRes = restTemplate.exchange(
                "/api/hrm/departments",
                HttpMethod.POST,
                new HttpEntity<>(deptReq, bearerHeaders(adminAccessToken)),
                DepartmentDto.class);
        assertEquals(HttpStatus.CREATED, deptRes.getStatusCode(), "Step 9: Admin creates department");

        CreateEmployeeRequest empReq = new CreateEmployeeRequest(
                "EMP-ACME-" + uniqueSuffix.substring(0, 4),
                "Robert Enterprise",
                "robert." + uniqueSuffix + "@acme.test",
                "Product Platform",
                "Staff Architect",
                EmployeeStatus.ACTIVE,
                LocalDate.now(),
                "+1 (555) 123-9999",
                "Hybrid",
                "New York, NY",
                "Jane Lead",
                "bg-zinc-800 text-zinc-100",
                new BigDecimal("99.00"),
                160);
        ResponseEntity<EmployeeDto> empRes = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.POST,
                new HttpEntity<>(empReq, bearerHeaders(adminAccessToken)),
                EmployeeDto.class);
        assertEquals(HttpStatus.CREATED, empRes.getStatusCode(), "Step 9: Admin creates employee");
        assertNotNull(empRes.getBody());
        UUID createdEmployeeId = empRes.getBody().id();

        // ---------------------------------------------------------------------
        // Step 10 & 11: Verify the appropriate user can access HRM data
        // ---------------------------------------------------------------------
        ResponseEntity<List<EmployeeDto>> empListRes = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(managerAccessToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, empListRes.getStatusCode(), "Step 11: MANAGER can view employee roster");
        assertTrue(empListRes.getBody().stream().anyMatch(e -> e.id().equals(createdEmployeeId)),
                "Created employee must be present in tenant roster");

        ResponseEntity<List<DepartmentDto>> deptListRes = restTemplate.exchange(
                "/api/hrm/departments",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(managerAccessToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, deptListRes.getStatusCode(), "Step 11: MANAGER can view departments");
        assertTrue(deptListRes.getBody().stream().anyMatch(d -> "Product Platform".equals(d.name())),
                "Created department must be present in tenant department list");

        // ---------------------------------------------------------------------
        // Step 12: Deactivate the user
        // ---------------------------------------------------------------------
        ResponseEntity<UserResponse> deactRes = restTemplate.exchange(
                "/api/users/" + teammateUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(adminAccessToken)),
                UserResponse.class);
        assertEquals(HttpStatus.OK, deactRes.getStatusCode(), "Step 12: Deactivate user should return 200");
        assertEquals(UserStatus.DISABLED, deactRes.getBody().status(), "Step 12: User status should be DISABLED");

        // ---------------------------------------------------------------------
        // Step 13: Verify the user is immediately blocked
        // ---------------------------------------------------------------------
        ResponseEntity<String> blockedTokenRes = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(managerAccessToken)),
                String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, blockedTokenRes.getStatusCode(),
                "Step 13: Deactivated user access token must be immediately rejected with 401");

        ResponseEntity<ErrorResponse> blockedLoginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantId, teammateEmail, teammatePassword),
                ErrorResponse.class);
        assertEquals(HttpStatus.UNAUTHORIZED, blockedLoginRes.getStatusCode(),
                "Step 13: Deactivated user cannot log in");

        ResponseEntity<ErrorResponse> blockedRefreshRes = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(managerTokens.refreshToken()),
                ErrorResponse.class);
        assertEquals(HttpStatus.UNAUTHORIZED, blockedRefreshRes.getStatusCode(),
                "Step 13: Deactivated user cannot refresh tokens");

        // ---------------------------------------------------------------------
        // Step 14: Verify historical records remain intact
        // ---------------------------------------------------------------------
        TenantContext.setTenant(tenantA.schemaName());
        try {
            User storedUser = userRepository.findById(teammateUserId).orElseThrow();
            assertEquals(UserStatus.DISABLED, storedUser.getStatus());
            assertEquals(teammateEmail, storedUser.getEmail());
            assertEquals(Role.MANAGER, storedUser.getRole());
        } finally {
            TenantContext.clear();
        }

        // ---------------------------------------------------------------------
        // Step 15: Verify audit logs
        // ---------------------------------------------------------------------
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Integer roleChangeAudit = jdbc.queryForObject(
                "SELECT count(*) FROM " + tenantA.schemaName() + ".audit_log WHERE action = 'USER_ROLE_CHANGE' AND outcome = 'SUCCESS'",
                Integer.class);
        assertTrue(roleChangeAudit != null && roleChangeAudit >= 1, "Step 15: USER_ROLE_CHANGE audit log must exist");

        Integer deactivateAudit = jdbc.queryForObject(
                "SELECT count(*) FROM " + tenantA.schemaName() + ".audit_log WHERE action = 'USER_DEACTIVATE' AND outcome = 'SUCCESS'",
                Integer.class);
        assertTrue(deactivateAudit != null && deactivateAudit >= 1, "Step 15: USER_DEACTIVATE audit log must exist");

        // ---------------------------------------------------------------------
        // Step 16: Log out
        // ---------------------------------------------------------------------
        ResponseEntity<Void> logoutRes = restTemplate.postForEntity(
                "/api/auth/logout",
                new LogoutRequest(adminTokens.refreshToken()),
                Void.class);
        assertEquals(HttpStatus.OK, logoutRes.getStatusCode(), "Step 16: Logout should return 200");

        // ---------------------------------------------------------------------
        // Step 17: Log back in
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> reLoginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantId, ownerEmail, ownerPassword),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, reLoginRes.getStatusCode(), "Step 17: Admin re-login should return 200");
        TokenResponse freshAdminTokens = reLoginRes.getBody();
        assertNotNull(freshAdminTokens);

        // ---------------------------------------------------------------------
        // Step 18: Verify refresh-token rotation
        // ---------------------------------------------------------------------
        ResponseEntity<TokenResponse> firstRotated = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(freshAdminTokens.refreshToken()),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, firstRotated.getStatusCode(), "Step 18: First refresh should rotate tokens");
        assertNotEquals(freshAdminTokens.refreshToken(), firstRotated.getBody().refreshToken());

        // Replay of consumed refresh token must fail with 401
        ResponseEntity<ErrorResponse> replayedRefresh = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(freshAdminTokens.refreshToken()),
                ErrorResponse.class);
        assertEquals(HttpStatus.UNAUTHORIZED, replayedRefresh.getStatusCode(),
                "Step 18: Replayed refresh token must be rejected with 401");

        // ---------------------------------------------------------------------
        // Step 19: Verify billing/test subscription behavior
        // ---------------------------------------------------------------------
        String activeAdminToken = firstRotated.getBody().accessToken();
        ResponseEntity<BillingSummaryResponse> billingRes = restTemplate.exchange(
                "/api/billing",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(activeAdminToken)),
                BillingSummaryResponse.class);
        assertEquals(HttpStatus.OK, billingRes.getStatusCode(), "Step 19: Retrieve billing summary");
        assertEquals("FREE", billingRes.getBody().plan());

        ResponseEntity<FeatureEntitlementsResponse> entitlementsRes = restTemplate.exchange(
                "/api/billing/entitlements",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(activeAdminToken)),
                FeatureEntitlementsResponse.class);
        assertEquals(HttpStatus.OK, entitlementsRes.getStatusCode(), "Step 19: Retrieve feature entitlements");
        assertEquals("STARTER", entitlementsRes.getBody().plan());

        // Test checkout initiation with test creator mock
        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/c/pay/cs_test_sample");
        when(checkoutSessionCreator.create(any())).thenReturn(mockSession);

        ResponseEntity<CreateCheckoutSessionResponse> checkoutRes = restTemplate.exchange(
                "/api/billing/checkout-session",
                HttpMethod.POST,
                new HttpEntity<>(new CreateCheckoutSessionRequest(PlanTier.PRO), bearerHeaders(activeAdminToken)),
                CreateCheckoutSessionResponse.class);
        assertEquals(HttpStatus.OK, checkoutRes.getStatusCode(), "Step 19: Checkout session created in test mode");
        assertEquals("https://checkout.stripe.com/c/pay/cs_test_sample", checkoutRes.getBody().checkoutUrl());

        // ---------------------------------------------------------------------
        // Step 20: Verify tenant isolation using a second tenant
        // ---------------------------------------------------------------------
        TenantRecord tenantB = provisionTenant("acme-b");
        String tenantBAdminEmail = "admin-b@acme-b.test";
        String tenantBAdminPw = "AdminBSecret123!";

        TenantContext.setTenant(tenantB.schemaName());
        try {
            User userB = User.newLocalUser(tenantBAdminEmail, passwordEncoder.encode(tenantBAdminPw), Role.ADMIN);
            userRepository.save(userB);
        } finally {
            TenantContext.clear();
        }

        ResponseEntity<TokenResponse> tenantBLogin = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenantB.tenantId(), tenantBAdminEmail, tenantBAdminPw),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, tenantBLogin.getStatusCode(), "Tenant B admin login");
        String tenantBToken = tenantBLogin.getBody().accessToken();

        // 20a. Tenant B admin cannot access Tenant A's created employee by ID -> 404
        ResponseEntity<String> crossEmpRes = restTemplate.exchange(
                "/api/hrm/employees/" + createdEmployeeId,
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tenantBToken)),
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, crossEmpRes.getStatusCode(),
                "Step 20: Tenant B cannot access Tenant A employee by ID");

        // 20b. Tenant B admin cannot deactivate Tenant A's user by ID -> 404
        ResponseEntity<String> crossDeactRes = restTemplate.exchange(
                "/api/users/" + teammateUserId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(tenantBToken)),
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, crossDeactRes.getStatusCode(),
                "Step 20: Tenant B cannot deactivate Tenant A user");

        // 20c. Tenant B employee roster contains NO Tenant A data
        ResponseEntity<List<EmployeeDto>> tenantBListRes = restTemplate.exchange(
                "/api/hrm/employees",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(tenantBToken)),
                new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, tenantBListRes.getStatusCode());
        assertFalse(tenantBListRes.getBody().stream().anyMatch(e -> e.id().equals(createdEmployeeId)),
                "Step 20: Tenant A employee must never leak into Tenant B employee list");
    }
}
