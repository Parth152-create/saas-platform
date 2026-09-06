package com.yourco.saas.auth;

import com.yourco.saas.auth.dto.LoginRequest;
import com.yourco.saas.auth.dto.TokenResponse;
import com.yourco.saas.domain.user.User;
import com.yourco.saas.domain.user.UserRepository;
import com.yourco.saas.domain.user.UserStatus;
import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRecord;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TenantRegistryService tenantRegistryService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(TenantRegistryService tenantRegistryService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.tenantRegistryService = tenantRegistryService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
            return ResponseEntity.ok(new TokenResponse(
                    tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
        } finally {
            TenantContext.clear();
        }
    }
}