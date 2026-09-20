package com.yourco.saas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourco.saas.auth.JwtAuthenticationFilter;
import com.yourco.saas.auth.JwtService;
import com.yourco.saas.common.exception.ErrorResponse;
import com.yourco.saas.tenant.TenantRegistryService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtDecoder jwtDecoder;
    private final TenantRegistryService tenantRegistryService;
    private final RateLimiterService rateLimiterService;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:}")
    private String additionalCorsOrigins;

    public SecurityConfig(JwtDecoder jwtDecoder,
                          TenantRegistryService tenantRegistryService,
                          RateLimiterService rateLimiterService,
                          JwtService jwtService) {
        this.jwtDecoder = jwtDecoder;
        this.tenantRegistryService = tenantRegistryService;
        this.rateLimiterService = rateLimiterService;
        this.jwtService = jwtService;
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
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .addHeaderWriter(new StaticHeadersWriter("Content-Security-Policy", "frame-ancestors 'none'"))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/metrics/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    log.warn("EntryPoint fired: {} {} — exception={}",
                            request.getMethod(), request.getRequestURI(), authException.getMessage());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (requestId == null) {
                        requestId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
                    }
                    ErrorResponse errorResponse = ErrorResponse.of(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Unauthorized",
                            "Full authentication is required to access this resource",
                            request.getRequestURI(),
                            null,
                            requestId
                    );
                    objectMapper.writeValue(response.getOutputStream(), errorResponse);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("AccessDeniedHandler fired: {} {} — exception={}",
                            request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
                    if (requestId == null) {
                        requestId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
                    }
                    ErrorResponse errorResponse = ErrorResponse.of(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Forbidden",
                            "Access is denied",
                            request.getRequestURI(),
                            null,
                            requestId
                    );
                    objectMapper.writeValue(response.getOutputStream(), errorResponse);
                }))
            .addFilterBefore(new CorrelationIdFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new RateLimitingFilter(rateLimiterService, objectMapper), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtDecoder, tenantRegistryService, jwtService),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> originPatterns = new ArrayList<>(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://localhost",
                "http://127.0.0.1"
        ));
        if (additionalCorsOrigins != null && !additionalCorsOrigins.isBlank()) {
            Arrays.stream(additionalCorsOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(originPatterns::add);
        }
        config.setAllowedOriginPatterns(originPatterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Request-ID"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}