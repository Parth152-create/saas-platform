package com.yourco.saas.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GoogleJwtDecoderUnitTest {

    @Test
    void testFixedGoogleValidators() {
        String clientId = "906646436149-dkke9514rgicekhqsglpavm8e5iffgug.apps.googleusercontent.com";

        // Issuer validator accepting both https://accounts.google.com and accounts.google.com
        OAuth2TokenValidator<Jwt> withIssuer = token -> {
            Object iss = token.getClaims().get("iss");
            if ("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Invalid issuer: " + iss, null));
        };

        // Audience validator using token.getAudience() which safely handles both String and List<String>
        OAuth2TokenValidator<Jwt> withAudience = token -> {
            List<String> audience = token.getAudience();
            if (audience != null && audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token", "Invalid audience: " + audience, null));
        };

        // Timestamp validator for exp/nbf
        OAuth2TokenValidator<Jwt> timestampValidator = new org.springframework.security.oauth2.jwt.JwtTimestampValidator();

        DelegatingOAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(timestampValidator, withIssuer, withAudience);

        // Case 1: String aud + https://accounts.google.com
        Jwt jwt1 = Jwt.withTokenValue("mock1")
                .header("alg", "RS256")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", clientId)
                .subject("12345")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result1 = combinedValidator.validate(jwt1);
        assertFalse(result1.hasErrors(), "Validation should succeed for String aud + https://accounts.google.com");

        // Case 2: String aud + accounts.google.com (without https://)
        Jwt jwt2 = Jwt.withTokenValue("mock2")
                .header("alg", "RS256")
                .claim("iss", "accounts.google.com")
                .claim("aud", clientId)
                .subject("12345")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result2 = combinedValidator.validate(jwt2);
        assertFalse(result2.hasErrors(), "Validation should succeed for String aud + accounts.google.com");

        // Case 3: List<String> aud
        Jwt jwt3 = Jwt.withTokenValue("mock3")
                .header("alg", "RS256")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", List.of(clientId, "other-client"))
                .subject("12345")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result3 = combinedValidator.validate(jwt3);
        assertFalse(result3.hasErrors(), "Validation should succeed for List aud");

        // Case 4: Wrong audience
        Jwt jwt4 = Jwt.withTokenValue("mock4")
                .header("alg", "RS256")
                .claim("iss", "https://accounts.google.com")
                .claim("aud", "wrong-client-id")
                .subject("12345")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result4 = combinedValidator.validate(jwt4);
        assertTrue(result4.hasErrors(), "Validation should fail for wrong audience");

        // Case 5: Wrong issuer
        Jwt jwt5 = Jwt.withTokenValue("mock5")
                .header("alg", "RS256")
                .claim("iss", "https://evil.com")
                .claim("aud", clientId)
                .subject("12345")
                .issuedAt(Instant.now().minusSeconds(10))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result5 = combinedValidator.validate(jwt5);
        assertTrue(result5.hasErrors(), "Validation should fail for wrong issuer");
    }
}
