package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.AcceptInviteRequest;
import com.yourco.saas.auth.dto.GoogleLoginRequest;
import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.LogoutRequest;
import com.yourco.saas.auth.dto.RefreshRequest;
import com.yourco.saas.auth.dto.SignupRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.common.audit.AuditLog;
import com.yourco.saas.common.audit.AuditLogRepository;
import com.yourco.saas.domain.user.AuthProvider;
import com.yourco.saas.domain.user.Role;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantOnboardingService;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final TenantRegistryService tenantRegistryService;
    private final TenantOnboardingService tenantOnboardingService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;
    private final JwtDecoder googleJwtDecoder;
    private final AuditLogRepository auditLogRepository;

    public AuthController(TenantRegistryService tenantRegistryService,
                           TenantOnboardingService tenantOnboardingService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtDecoder jwtDecoder,
                           @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
                           AuditLogRepository auditLogRepository) {
        this.tenantRegistryService = tenantRegistryService;
        this.tenantOnboardingService = tenantOnboardingService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtDecoder = jwtDecoder;
        this.googleJwtDecoder = googleJwtDecoder;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@RequestBody @Valid SignupRequest request) {
        TenantRecord tenant = tenantOnboardingService.onboardTenant(request.tenantId(), "FREE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tenant ID already taken: " + request.tenantId()));

        TenantContext.setTenant(tenant.schemaName());
        try {
            User admin = User.newLocalUser(
                    request.email(), passwordEncoder.encode(request.password()), Role.SUPER_ADMIN);
            userRepository.save(admin);

            TokenPair tokens = jwtService.issueTokens(admin, tenant.tenantId());

            auditLogRepository.save(new AuditLog(
                    admin.getId(), admin.getRole().name(), "TENANT_SIGNUP", "SUCCESS", null));

            return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<TokenResponse> acceptInvite(@RequestBody @Valid AcceptInviteRequest request) {
        TenantRecord tenant = tenantRegistryService.findByTenantId(request.tenantId())
                .filter(t -> "ACTIVE".equals(t.status()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid invite"));

        TenantContext.setTenant(tenant.schemaName());
        try {
            User user = userRepository.findByInviteToken(request.token())
                    .filter(u -> u.getStatus() == UserStatus.INVITED)
                    .filter(u -> u.getInviteTokenExpiresAt() != null
                            && u.getInviteTokenExpiresAt().isAfter(Instant.now()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired invite"));

            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setAuthProvider(AuthProvider.LOCAL);
            user.setStatus(UserStatus.ACTIVE);
            user.clearInviteToken();
            userRepository.save(user);

            TokenPair tokens = jwtService.issueTokens(user, tenant.tenantId());

            auditLogRepository.save(new AuditLog(
                    user.getId(), user.getRole().name(), "INVITE_ACCEPTED", "SUCCESS", null));

            return ResponseEntity.ok(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
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
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid Google token provided: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
        } catch (Exception e) {
            log.error("Error decoding Google token", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to verify Google token: " + e.getMessage());
        }

        String email = googleJwt.getClaimAsString("email");
        String googleSubject = googleJwt.getSubject();
        Object emailVerifiedClaim = googleJwt.getClaim("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(emailVerifiedClaim)
                || "true".equalsIgnoreCase(String.valueOf(emailVerifiedClaim));

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

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest request) {
        try {
            Jwt jwt = jwtDecoder.decode(request.refreshToken());
            String jti = jwt.getId();
            if (jti != null) {
                jwtService.revokeRefreshToken(jti);
            }
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null) {
                tenantRegistryService.findByTenantId(tenantId)
                        .filter(t -> "ACTIVE".equals(t.status()))
                        .ifPresent(tenant -> {
                            TenantContext.setTenant(tenant.schemaName());
                            try {
                                UUID userId = UUID.fromString(jwt.getSubject());
                                auditLogRepository.save(new AuditLog(
                                        userId, "USER", "USER_LOGOUT", "SUCCESS", null));
                            } catch (Exception ignored) {
                            } finally {
                                TenantContext.clear();
                            }
                        });
            }
        } catch (JwtException e) {
            log.debug("Logout called with expired or invalid refresh token: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}