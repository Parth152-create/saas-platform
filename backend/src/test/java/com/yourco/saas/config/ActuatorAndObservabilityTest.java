package com.yourco.saas.config;

import com.yourco.saas.integration.IntegrationTestBase;
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
class ActuatorAndObservabilityTest extends IntegrationTestBase {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void actuatorHealthEndpointIsPubliclyAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP"));
    }

    @Test
    void correlationIdIsPropagatedOrGenerated() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Request-ID", "custom-req-id-12345");

        ResponseEntity<String> response = restTemplate.exchange(
                "/actuator/health",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("custom-req-id-12345", response.getHeaders().getFirst("X-Request-ID"));

        // If not provided, one is generated
        ResponseEntity<String> generatedResponse = restTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, generatedResponse.getStatusCode());
        assertNotNull(generatedResponse.getHeaders().getFirst("X-Request-ID"));
        assertFalse(generatedResponse.getHeaders().getFirst("X-Request-ID").isBlank());
    }

    @Test
    void actuatorMetricsEndpointIsAccessibleAndExposesMetrics() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("names"), "Metrics endpoint should list available metric names");
    }
}
