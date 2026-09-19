package com.yourco.saas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RateLimiterTest {

    private RateLimitProperties properties;
    private RateLimiterService rateLimiterService;
    private RateLimitingFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setEnabled(true);
        properties.setAuthRequestsPerMinute(5);
        properties.setApiRequestsPerMinute(10);

        // StringRedisTemplate mock will fail or return null, triggering in-memory fallback
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        rateLimiterService = new RateLimiterService(redisTemplate, properties);

        objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        filter = new RateLimitingFilter(rateLimiterService, objectMapper);
    }

    @Test
    void allowsRequestsUnderLimit() {
        String clientIp = "192.168.1.100";
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiterService.isAllowed(clientIp, true), "Request " + (i + 1) + " should be allowed");
        }
        // 6th request exceeds limit of 5
        assertFalse(rateLimiterService.isAllowed(clientIp, true), "Request 6 should be rejected");
    }

    @Test
    void disabledRateLimiterAllowsUnlimited() {
        properties.setEnabled(false);
        String clientIp = "10.0.0.1";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiterService.isAllowed(clientIp, true));
        }
    }

    @Test
    void filterRejectsWith429WhenRateLimitExceeded() throws Exception {
        String clientIp = "172.16.0.5";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(clientIp);
        FilterChain chain = mock(FilterChain.class);

        // Exhaust the 5 allowed requests
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(request, res, chain);
            assertEquals(200, res.getStatus());
        }

        // 6th request should hit 429
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);
        filter.doFilter(request, blockedRes, blockedChain);

        assertEquals(429, blockedRes.getStatus());
        assertEquals("60", blockedRes.getHeader("Retry-After"));
        assertTrue(blockedRes.getContentAsString().contains("Rate limit exceeded"));
        verify(blockedChain, never()).doFilter(request, blockedRes);
    }

    @Test
    void actuatorEndpointsBypassRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("10.10.10.10");
        FilterChain chain = mock(FilterChain.class);

        // Even with many requests, actuator is never blocked
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }
    }
}
