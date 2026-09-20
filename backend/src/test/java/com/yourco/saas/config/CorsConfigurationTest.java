package com.yourco.saas.config;

import com.yourco.saas.integration.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestRestTemplate
class CorsConfigurationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Local development origin http://localhost:5173 is allowed with credentials")
    void localDevelopmentOriginAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:5173");

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("http://localhost:5173", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("Local development origin http://127.0.0.1:3000 is allowed with credentials")
    void localIpOriginAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://127.0.0.1:3000");

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("http://127.0.0.1:3000", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("Disallowed origin is rejected during preflight with 403 Forbidden and no allow-origin")
    void disallowedOriginRejectedOnPreflight() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://malicious-external-site.com");
        headers.set("Access-Control-Request-Method", "POST");
        headers.set("Access-Control-Request-Headers", "Authorization, Content-Type");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.OPTIONS,
                new HttpEntity<>(headers),
                String.class
        );

        // Preflight from disallowed origin must be rejected by Spring CORS filter
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNull(response.getHeaders().getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Preflight with allowed origin exposes explicit methods and allowed headers")
    void preflightOptionsWithAllowedOriginSucceeds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:5173");
        headers.set("Access-Control-Request-Method", "POST");
        headers.set("Access-Control-Request-Headers", "Authorization, Content-Type, X-Request-ID");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/auth/login",
                HttpMethod.OPTIONS,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("http://localhost:5173", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeaders().getFirst("Access-Control-Allow-Credentials"));

        String allowMethods = response.getHeaders().getFirst("Access-Control-Allow-Methods");
        assertNotNull(allowMethods);
        assertTrue(allowMethods.contains("POST"));
        assertTrue(allowMethods.contains("GET"));
        assertTrue(allowMethods.contains("OPTIONS"));

        String allowHeaders = response.getHeaders().getFirst("Access-Control-Allow-Headers");
        assertNotNull(allowHeaders);
        assertTrue(allowHeaders.contains("Authorization"));
        assertTrue(allowHeaders.contains("Content-Type"));
        assertTrue(allowHeaders.contains("X-Request-ID"));
    }

    @Test
    @DisplayName("WebSocket handshake with disallowed origin is rejected with 403 Forbidden")
    void websocketHandshakeDisallowedOriginRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://malicious-external-site.com");
        headers.set("Upgrade", "websocket");
        headers.set("Connection", "Upgrade");
        headers.set("Sec-WebSocket-Version", "13");
        headers.set("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");

        ResponseEntity<String> response = restTemplate.exchange(
                "/ws",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "WebSocket handshake from disallowed origin must be rejected with 403 Forbidden");
    }
}

