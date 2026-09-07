package com.yourco.saas.auth;

import com.yourco.saas.tenant.TenantContext;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtDecoder jwtDecoder;
    private final TenantRegistryService tenantRegistryService;

    public JwtAuthenticationFilter(JwtDecoder jwtDecoder, TenantRegistryService tenantRegistryService) {
        this.jwtDecoder = jwtDecoder;
        this.tenantRegistryService = tenantRegistryService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            log.debug("JwtAuthenticationFilter: {} {} — token present: {}",
                    request.getMethod(), request.getRequestURI(), token != null);
            if (token != null) {
                authenticate(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void authenticate(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null) {
                tenantRegistryService.findByTenantId(tenantId)
                        .ifPresent(t -> TenantContext.setTenant(t.schemaName()));
            }

            String role = jwt.getClaimAsString("role");
            List<GrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            var authentication = new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated subject {} for tenant {}, authorities={}", jwt.getSubject(), tenantId, authorities);
        } catch (JwtException e) {
            log.warn("JWT rejected: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
    }
}