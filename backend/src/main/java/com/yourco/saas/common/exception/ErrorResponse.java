package com.yourco.saas.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details,
        String requestId) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> details) {
        return new ErrorResponse(Instant.now(), status, error, message, path, details, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> details, String requestId) {
        return new ErrorResponse(Instant.now(), status, error, message, path, details, requestId);
    }
}