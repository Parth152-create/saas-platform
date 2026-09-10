package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.GoogleLoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.exception.ErrorResponse;
import com.yourco.saas.domain.user.AuthProvider;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Codifies Phase 3 Step 4's design decisions - invite-only linking,
 * email_verified enforcement, and preserving LOCAL authProvider on dual
 * login - which were previously only verified once by hand with a real
 * Google account.
 *
 * googleJwtDecoder normally calls Google's real JWKS endpoint over the
 * network. MockitoBean(name = "googleJwtDecoder") swaps it for a Mockito
 * mock scoped to this test class, so these tests are fast, offline, and
 * can drive claim shapes (unverified email, mismatched subject, etc.)
 * that would be impractical to produce with a real Google account. Only
 * googleJwtDecoder is replaced - the main jwtDecoder bean (used to verify
 * OUR issued tokens below) is untouched and real.
 */
@AutoConfigureTestRestTemplate
class GoogleLoginFlowTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean(name = "googleJwtDecoder")
    JwtDecoder googleJwtDecoder;

    private static Jwt fakeGoogleJwt(String subject, String email, boolean emailVerified) {
        Jwt.Builder builder = Jwt.withTokenValue("fake-google-id-token")
                .header("alg", "RS256")
                .claim("sub", subject)
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        return builder.build();
    }

    private User findUser(String schema, String email) {
        TenantContext.setTenant(schema);
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        TenantContext.clear();
        return user.orElseThrow();
    }

    @Test
    void firstTimeGoogleLoginLinksInvitedUserAndActivatesAccount() {
        TenantRecord tenant = provisionTenant("google-invite");
        String email = "invited@example.test";
        String googleSubject = "google-sub-" + UUID.randomUUID();

        TenantContext.setTenant(tenant.schemaName());
        User invited = UserTestFactory.localUser(email, Role.USER);
        invited.setStatus(UserStatus.INVITED);
        userRepository.save(invited);
        TenantContext.clear();

        when(googleJwtDecoder.decode(anyString())).thenReturn(fakeGoogleJwt(googleSubject, email, true));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), TokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().accessToken());

        User updated = findUser(tenant.schemaName(), email);
        assertEquals(UserStatus.ACTIVE, updated.getStatus(), "INVITED should flip to ACTIVE on first Google claim");
        assertEquals(AuthProvider.GOOGLE, updated.getAuthProvider());
        assertEquals(googleSubject, updated.getGoogleSubject());

        Integer auditCount = new JdbcTemplate(dataSource).queryForObject(
                "SELECT count(*) FROM " + tenant.schemaName()
                        + ".audit_log WHERE action = 'USER_LOGIN_GOOGLE' AND outcome = 'SUCCESS'",
                Integer.class);
        assertEquals(1, auditCount);
    }

    @Test
    void existingGoogleUserLogsInAgainWithoutChanges() {
        TenantRecord tenant = provisionTenant("google-repeat");
        String email = "repeat@example.test";
        String googleSubject = "google-sub-" + UUID.randomUUID();

        TenantContext.setTenant(tenant.schemaName());
        User existing = UserTestFactory.localUser(email, Role.USER);
        existing.setStatus(UserStatus.ACTIVE);
        existing.setAuthProvider(AuthProvider.GOOGLE);
        existing.setGoogleSubject(googleSubject);
        userRepository.save(existing);
        TenantContext.clear();

        when(googleJwtDecoder.decode(anyString())).thenReturn(fakeGoogleJwt(googleSubject, email, true));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), TokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        User unchanged = findUser(tenant.schemaName(), email);
        assertEquals(UserStatus.ACTIVE, unchanged.getStatus());
        assertEquals(AuthProvider.GOOGLE, unchanged.getAuthProvider());
        assertEquals(googleSubject, unchanged.getGoogleSubject());
    }

    @Test
    void dualLoginPreservesLocalAuthProviderWhenLinkingGoogle() {
        TenantRecord tenant = provisionTenant("google-dual");
        String email = "dual@example.test";
        String googleSubject = "google-sub-" + UUID.randomUUID();

        TenantContext.setTenant(tenant.schemaName());
        User localUser = UserTestFactory.localUser(email, Role.USER);
        localUser.setPasswordHash(passwordEncoder.encode("password123"));
        localUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(localUser);
        TenantContext.clear();

        when(googleJwtDecoder.decode(anyString())).thenReturn(fakeGoogleJwt(googleSubject, email, true));

        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), TokenResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        User updated = findUser(tenant.schemaName(), email);
        assertEquals(AuthProvider.LOCAL, updated.getAuthProvider());
        assertEquals(googleSubject, updated.getGoogleSubject());
        assertNotNull(updated.getPasswordHash());
    }

    @Test
    void mismatchedGoogleSubjectIsRejected() {
        TenantRecord tenant = provisionTenant("google-mismatch");
        String email = "mismatch@example.test";

        TenantContext.setTenant(tenant.schemaName());
        User existing = UserTestFactory.localUser(email, Role.USER);
        existing.setStatus(UserStatus.ACTIVE);
        existing.setAuthProvider(AuthProvider.GOOGLE);
        existing.setGoogleSubject("original-google-sub");
        userRepository.save(existing);
        TenantContext.clear();

        when(googleJwtDecoder.decode(anyString())).thenReturn(fakeGoogleJwt("different-google-sub", email, true));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), ErrorResponse.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Account linked to a different Google identity", response.getBody().message());
    }

    @Test
    void unverifiedEmailIsRejected() {
        TenantRecord tenant = provisionTenant("google-unverified");

        when(googleJwtDecoder.decode(anyString()))
                .thenReturn(fakeGoogleJwt("some-sub", "unverified@example.test", false));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Google account email not verified", response.getBody().message());
    }

    @Test
    void noInvitationForEmailReturns403() {
        TenantRecord tenant = provisionTenant("google-noinvite");

        when(googleJwtDecoder.decode(anyString()))
                .thenReturn(fakeGoogleJwt("some-sub", "nobody-invited@example.test", true));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("any-id-token", tenant.tenantId()), ErrorResponse.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No invitation found for this email in this tenant", response.getBody().message());
    }

    @Test
    void invalidGoogleTokenReturns401() {
        TenantRecord tenant = provisionTenant("google-badtoken");

        when(googleJwtDecoder.decode(anyString())).thenThrow(new BadJwtException("signature verification failed"));

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/auth/google", new GoogleLoginRequest("garbage-token", tenant.tenantId()), ErrorResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid Google token", response.getBody().message());
    }
}
