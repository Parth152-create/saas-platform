package com.yourco.saas.auth;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {}