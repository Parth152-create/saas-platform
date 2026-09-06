package com.yourco.saas.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.config.crypto.RsaKeyConversionServicePostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

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
}