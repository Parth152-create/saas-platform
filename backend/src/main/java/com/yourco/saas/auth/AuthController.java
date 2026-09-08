package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.GoogleLoginRequest;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.RefreshRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.user.AuthProvider;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TenantRegistryService tenantRegistryService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final JwtDecoder googleJwtDecoder;
    private final AuditLogRepository auditLogRepository;

    public AuthController(TenantRegistryService tenantRegistryService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtDecoder jwtDecoder,
                           @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
                           AuditLogRepository auditLogRepository) {
        this.tenantRegistryService = tenantRegistryService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
        this.googleJwtDecoder = googleJwtDecoder;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TenantRecord tenant = tenantRegistryService.findByTenantId(request.tenantId())
                .filter(t -> "ACTIVE".equals(t.status()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = userRepository.findByEmailIgnoreCase(request.email())
                    .filter(u -> u.getPasswordHash() != null)
                    .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

            TokenPair tokens = jwtService.issueTokens(user, tenant.tenantId());

            auditLogRepository.save(new AuditLog(
                    user.getId(), user.getRole().name(), "USER_LOGIN_PASSWORD", "SUCCESS", null));

            return ResponseEntity.ok(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        Jwt googleJwt;
        try {
            googleJwt = googleJwtDecoder.decode(request.idToken());
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        }

        String email = googleJwt.getClaimAsString("email");
        String googleSubject = googleJwt.getSubject();
        boolean emailVerified = Boolean.TRUE.equals(googleJwt.getClaim("email_verified"));

        if (email == null || !emailVerified) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account email not verified");
        }

        TenantRecord tenant = tenantRegistryService.findByTenantId(request.tenantId())
                .filter(t -> "ACTIVE".equals(t.status()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid tenant"));

        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .filter(u -> u.getStatus() != UserStatus.DISABLED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "No invitation found for this email in this tenant"));

            if (user.getGoogleSubject() == null) {
                user.setGoogleSubject(googleSubject);
                if (user.getStatus() == UserStatus.INVITED) {
                    user.setStatus(UserStatus.ACTIVE);
                    user.setAuthProvider(AuthProvider.GOOGLE);
                }
                userRepository.save(user);
            } else if (!user.getGoogleSubject().equals(googleSubject)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account linked to a different Google identity");
            }

            TokenPair tokens = jwtService.issueTokens(user, tenant.tenantId());

            auditLogRepository.save(new AuditLog(
                    user.getId(), user.getRole().name(), "USER_LOGIN_GOOGLE", "SUCCESS", null));

            return ResponseEntity.ok(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(request.refreshToken());
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        if (!"refresh".equals(jwt.getClaimAsString("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String jti = jwt.getId();
        if (!jwtService.isRefreshTokenValid(jti)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked or already used");
        }

        String tenantId = jwt.getClaimAsString("tenant_id");
        TenantRecord tenant = tenantRegistryService.findByTenantId(tenantId)
                .filter(t -> "ACTIVE".equals(t.status()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        TenantContext.setTenant(tenant.schemaName());
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            User user = userRepository.findById(userId)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

            jwtService.revokeRefreshToken(jti); // rotation: this refresh token is now single-use
            TokenPair tokens = jwtService.issueTokens(user, tenant.tenantId());
            return ResponseEntity.ok(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
    }
}