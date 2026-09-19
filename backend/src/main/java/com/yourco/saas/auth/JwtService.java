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
        try {
            redisTemplate.opsForSet().add("user_refresh:" + user.getId(), refreshJti);
            redisTemplate.expire("user_refresh:" + user.getId(), props.getRefreshTokenTtl());
        } catch (Exception ignored) {
        }

        return new TokenPair(accessToken, refreshToken, props.getAccessTokenTtl().toSeconds());
    }

    public boolean isRefreshTokenValid(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("refresh:" + jti));
    }

    public void revokeRefreshToken(String jti) {
        try {
            String userId = redisTemplate.opsForValue().get("refresh:" + jti);
            if (userId != null) {
                redisTemplate.opsForSet().remove("user_refresh:" + userId, jti);
            }
        } catch (Exception ignored) {
        }
        redisTemplate.delete("refresh:" + jti);
    }

    public void revokeUserRefreshTokens(UUID userId) {
        if (userId == null) return;
        try {
            String userKey = "user_refresh:" + userId;
            java.util.Set<String> jtis = redisTemplate.opsForSet().members(userKey);
            if (jtis != null && !jtis.isEmpty()) {
                for (String jti : jtis) {
                    redisTemplate.delete("refresh:" + jti);
                }
            }
            redisTemplate.delete(userKey);
        } catch (Exception ignored) {
        }
    }

    public void markUserDisabled(UUID userId) {
        if (userId == null) return;
        revokeUserRefreshTokens(userId);
        try {
            redisTemplate.opsForValue().set("user_disabled:" + userId, "true", props.getRefreshTokenTtl());
        } catch (Exception ignored) {
        }
    }

    public boolean isUserDisabled(UUID userId) {
        if (userId == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey("user_disabled:" + userId));
        } catch (Exception e) {
            return false;
        }
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
