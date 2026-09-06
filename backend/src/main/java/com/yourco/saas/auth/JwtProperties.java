package com.yourco.saas.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(7);

    public RSAPublicKey getPublicKey() { return publicKey; }
    public void setPublicKey(RSAPublicKey publicKey) { this.publicKey = publicKey; }
    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public void setPrivateKey(RSAPrivateKey privateKey) { this.privateKey = privateKey; }
    public Duration getAccessTokenTtl() { return accessTokenTtl; }
    public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
    public Duration getRefreshTokenTtl() { return refreshTokenTtl; }
    public void setRefreshTokenTtl(Duration refreshTokenTtl) { this.refreshTokenTtl = refreshTokenTtl; }
}