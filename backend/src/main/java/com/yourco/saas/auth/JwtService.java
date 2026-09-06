package com.yourco.saas.auth;

import com.yourco.saas.domain.user.User;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;
    private final StringRedisTemplate redisTemplate;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties props, StringRedisTemplate redisTemplate) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    public TokenPair issueTokens(User user, String tenantId) {
        Instant now = Instant.now();

        String accessToken = encode(JwtClaimsSet.builder()
                .issuer("saas-platform")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.getAccessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .claim("tenant_id", tenantId)
                .claim("role", user.getRole().name())
                .build());

        String refreshJti = UUID.randomUUID().toString();
        String refreshToken = encode(JwtClaimsSet.builder()
                .issuer("saas-platform")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.getRefreshTokenTtl()))
                .id(refreshJti)
                .claim("tenant_id", tenantId)
                .claim("type", "refresh")
                .build());

        redisTemplate.opsForValue().set(
                "refresh:" + refreshJti, user.getId().toString(), props.getRefreshTokenTtl());

        return new TokenPair(accessToken, refreshToken, props.getAccessTokenTtl().toSeconds());
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}