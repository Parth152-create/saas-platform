package com.yourco.saas.common.exception;

import com.yourco.saas.config.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        MDC.put(CorrelationIdFilter.MDC_KEY, "test-corr-id-456");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handles404NoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/api/test", "Resource not found");
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handles405MethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().status());
        assertEquals("Method Not Allowed", response.getBody().error());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handles415UnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("application/xml");
        ResponseEntity<ErrorResponse> response = handler.handleMediaTypeNotSupported(ex, request);

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(415, response.getBody().status());
        assertEquals("Unsupported Media Type", response.getBody().error());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handles400MalformedJson() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Cannot deserialize", new MockHttpInputMessage(new byte[0]));
        ResponseEntity<ErrorResponse> response = handler.handleMessageNotReadable(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Malformed JSON request payload", response.getBody().message());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handles429RateLimitExceeded() {
        RateLimitExceededException ex = new RateLimitExceededException("Rate limit exceeded", 60);
        ResponseEntity<ErrorResponse> response = handler.handleRateLimit(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getBody());
        assertEquals(429, response.getBody().status());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handles500WithoutLeakingInternalDetailsOrStackTraces() {
        RuntimeException internalError = new RuntimeException("CRITICAL: SELECT * FROM secret_table WHERE pw = 'leak'");
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(internalError, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().message());
        assertFalse(response.getBody().message().contains("secret_table"),
                "Production error message must never leak SQL or internal details");
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }

    @Test
    void handlesResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Resource already exists");
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Resource already exists", response.getBody().message());
        assertEquals("test-corr-id-456", response.getBody().requestId());
    }
}
