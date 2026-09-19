package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.LogoutRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AutoConfigureTestRestTemplate
class AuthLogoutTest extends IntegrationTestBase {

    private static final String RAW_PASSWORD = "Password123!";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void logoutRevokesRefreshTokenInRedisAndPreventsReuse() {
        TenantRecord tenant = provisionTenant("logout-test");
        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = UserTestFactory.localUser("logout@example.test", Role.USER);
            user.setPasswordHash(passwordEncoder.encode(RAW_PASSWORD));
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } finally {
            TenantContext.clear();
        }

        // 1. Log in
        ResponseEntity<TokenResponse> loginRes = restTemplate.postForEntity(
                "/api/auth/login",
                new LoginRequest(tenant.tenantId(), "logout@example.test", RAW_PASSWORD),
                TokenResponse.class);
        assertEquals(HttpStatus.OK, loginRes.getStatusCode());
        TokenResponse tokens = loginRes.getBody();
        assertNotNull(tokens);

        Jwt refreshJwt = jwtDecoder.decode(tokens.refreshToken());
        String jti = refreshJwt.getId();
        assertTrue(Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + jti)),
                "Refresh token should be present in Redis before logout");

        // 2. Call /api/auth/logout
        ResponseEntity<Void> logoutRes = restTemplate.postForEntity(
                "/api/auth/logout",
                new LogoutRequest(tokens.refreshToken()),
                Void.class);
        assertEquals(HttpStatus.OK, logoutRes.getStatusCode());

        // 3. Verify revoked in Redis
        assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + jti)),
                "Refresh token should be deleted from Redis after logout");

        // 4. Attempt to refresh with revoked token -> 401 Unauthorized
        ResponseEntity<ErrorResponse> refreshRes = restTemplate.postForEntity(
                "/api/auth/refresh",
                new RefreshRequest(tokens.refreshToken()),
                ErrorResponse.class);
        assertEquals(HttpStatus.UNAUTHORIZED, refreshRes.getStatusCode());
        assertNotNull(refreshRes.getBody());
        assertEquals("Refresh token has been revoked or already used", refreshRes.getBody().message());
    }

    @Test
    void logoutWithInvalidTokenIsIdempotentAndReturns200() {
        ResponseEntity<Void> logoutRes = restTemplate.postForEntity(
                "/api/auth/logout",
                new LogoutRequest("invalid-or-garbage-token"),
                Void.class);
        assertEquals(HttpStatus.OK, logoutRes.getStatusCode());
    }
}
