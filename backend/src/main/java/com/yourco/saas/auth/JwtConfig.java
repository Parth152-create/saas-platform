package com.yourco.saas.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.config.crypto.RsaKeyConversionServicePostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public static RsaKeyConversionServicePostProcessor rsaKeyConversionServicePostProcessor() {
        return new RsaKeyConversionServicePostProcessor();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties props) {
        RSAKey rsaKey = new RSAKey.Builder(props.getPublicKey())
                .privateKey(props.getPrivateKey())
                .keyID(UUID.randomUUID().toString())
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    // unused until Step 2, defined here since it shares the same keypair
    @Bean
    JwtDecoder jwtDecoder(JwtProperties props) {
        return NimbusJwtDecoder.withPublicKey(props.getPublicKey()).build();
    }

    @Bean
    JwtDecoder googleJwtDecoder(@Value("${app.google.client-id}") String googleClientId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer("https://accounts.google.com");
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                "aud", aud -> aud != null && aud.contains(googleClientId));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return decoder;
    }
}
