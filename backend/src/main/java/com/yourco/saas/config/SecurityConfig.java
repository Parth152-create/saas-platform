package com.yourco.saas.config;

import com.yourco.saas.auth.JwtAuthenticationFilter;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtDecoder jwtDecoder;
    private final TenantRegistryService tenantRegistryService;

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}")
    private String additionalCorsOrigins;

    public SecurityConfig(JwtDecoder jwtDecoder, TenantRegistryService tenantRegistryService) {
        this.jwtDecoder = jwtDecoder;
        this.tenantRegistryService = tenantRegistryService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("SUPER_ADMIN").implies("ADMIN")
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("USER")
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/metrics/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/webhooks/**").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                log.warn("EntryPoint fired: {} {} — exception={}, contextAuth={}",
                        request.getMethod(), request.getRequestURI(),
                        authException, SecurityContextHolder.getContext().getAuthentication());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            })
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                log.warn("AccessDeniedHandler fired: {} {} — exception={}, contextAuth={}",
                        request.getMethod(), request.getRequestURI(),
                        accessDeniedException, SecurityContextHolder.getContext().getAuthentication());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            }))
        .addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtDecoder, tenantRegistryService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        java.util.List<String> originPatterns = new java.util.ArrayList<>(java.util.List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://localhost",
                "http://127.0.0.1"
        ));
        if (additionalCorsOrigins != null && !additionalCorsOrigins.isBlank()) {
            java.util.Arrays.stream(additionalCorsOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(originPatterns::add);
        }
        config.setAllowedOriginPatterns(originPatterns);
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setExposedHeaders(java.util.List.of("X-Request-ID"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}