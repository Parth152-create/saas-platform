package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.RefreshRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.exception.ErrorResponse;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codifies what was hand-verified with curl in Phase 3 Step 1: POST
 * /api/auth/login success/failure paths, and /refresh single-use rotation.
 * Uses TestRestTemplate to go through the REAL HTTP stack - filter chain,
 * @Valid validation, GlobalExceptionHandler - not just calling
 * AuthController methods directly.
 * <p>
 * Boot 4 no longer auto-provides TestRestTemplate on @SpringBootTest -
 * @AutoConfigureTestRestTemplate opts back in explicitly.
 */
@AutoConfigureTestRestTemplate
class AuthLoginFlowTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "password123";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtDecoder jwtDecoder;

    @Autowired
    StringRedisTemplate redisTemplate;

    private User createLocalUser(String schema, String email, UserStatus status) {
        TenantContext.setTenant(schema);
        User user = UserTestFactory.localUser(email, Role.USER);
        user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
        user.setStatus(status);
        User saved = userRepository.save(user);
        TenantContext.clear();
        return saved;
    }

    @Test
    void successfulLoginReturnsValidTokensAndAuditLogEntry() {
        TenantRecord tenant = provisionTenant("login-ok");
        User user = createLocalUser(tenant.schemaName(), "login-ok@example.test", UserStatus.ACTIVE);

        LoginRequest request = new LoginRequest(tenant.tenantId(), "login-ok@example.test", RAW_PASSWORD);
        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, TokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        TokenResponse body = response.getBody();
        assertNotNull(body);
        assertNotNull(body.accessToken());
        assertNotNull(body.refreshToken());
        assertTrue(body.expiresIn() > 0);

        Jwt accessJwt = jwtDecoder.decode(body.accessToken());
        assertEquals(user.getId().toString(), accessJwt.getSubject());
        assertEquals(tenant.tenantId(), accessJwt.getClaimAsString("tenant_id"));
        assertEquals("USER", accessJwt.getClaimAsString("role"));

        Jwt refreshJwt = jwtDecoder.decode(body.refreshToken());
        assertEquals("refresh", refreshJwt.getClaimAsString("type"));
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + refreshJwt.getId())),
                "Refresh token's jti should be stored in Redis for later validation/rotation");

        Integer auditCount = new JdbcTemplate(dataSource).queryForObject(
                "SELECT count(*) FROM " + tenant.schemaName()
                        + ".audit_log WHERE action = 'USER_LOGIN_PASSWORD' AND outcome = 'SUCCESS'",
                Integer.class);
        assertEquals(1, auditCount);
    }

    @Test
    void wrongPasswordReturns401WithGenericMessage() {
        TenantRecord tenant = provisionTenant("login-badpw");
        createLocalUser(tenant.schemaName(), "badpw@example.test", UserStatus.ACTIVE);

        LoginRequest request = new LoginRequest(tenant.tenantId(), "badpw@example.test", "wrong-password");
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().message());
    }

    @Test
    void unknownEmailReturns401WithSameGenericMessage() {
        TenantRecord tenant = provisionTenant("login-noemail");

        LoginRequest request = new LoginRequest(tenant.tenantId(), "nobody@example.test", RAW_PASSWORD);
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().message());
    }

    @Test
    void unknownTenantReturns401WithSameGenericMessage() {
        LoginRequest request = new LoginRequest("no-such-tenant-slug", "anyone@example.test", RAW_PASSWORD);
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().message());
    }

    @Test
    void disabledUserReturns401WithSameGenericMessage() {
        TenantRecord tenant = provisionTenant("login-disabled");
        createLocalUser(tenant.schemaName(), "disabled@example.test", UserStatus.DISABLED);

        LoginRequest request = new LoginRequest(tenant.tenantId(), "disabled@example.test", RAW_PASSWORD);
        ResponseEntity<ErrorResponse> response =
                restTemplate.postForEntity("/api/auth/login", request, ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().message());
    }

    @Test
    void refreshTokenRotationIsSingleUse() {
        TenantRecord tenant = provisionTenant("login-refresh");
        createLocalUser(tenant.schemaName(), "refresh@example.test", UserStatus.ACTIVE);

        TokenResponse initialTokens = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), "refresh@example.test", RAW_PASSWORD),
                TokenResponse.class).getBody();
        assertNotNull(initialTokens);

        ResponseEntity<TokenResponse> firstRefresh = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(initialTokens.refreshToken()),
                TokenResponse.class);

        assertEquals(HttpStatus.OK, firstRefresh.getStatusCode());
        TokenResponse rotatedTokens = firstRefresh.getBody();
        assertNotNull(rotatedTokens);
        assertNotEquals(initialTokens.accessToken(), rotatedTokens.accessToken());
        assertNotEquals(initialTokens.refreshToken(), rotatedTokens.refreshToken());

        ResponseEntity<ErrorResponse> secondRefresh = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(initialTokens.refreshToken()),
                ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, secondRefresh.getStatusCode());
        assertEquals("Refresh token has been revoked or already used", secondRefresh.getBody().message());
    }
}