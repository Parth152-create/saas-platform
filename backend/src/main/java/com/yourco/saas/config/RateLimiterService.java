package com.yourco.saas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    // Fallback in-memory counters if Redis is temporarily unreachable
    private final ConcurrentHashMap<String, CounterWindow> memoryCounters = new ConcurrentHashMap<>();

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public boolean isAllowed(String clientIp, boolean isAuthEndpoint) {
        if (!properties.isEnabled()) {
            return true;
        }

        int limit = isAuthEndpoint
                ? properties.getAuthRequestsPerMinute()
                : properties.getApiRequestsPerMinute();

        String key = "ratelimit:" + (isAuthEndpoint ? "auth:" : "api:") + clientIp;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return count != null && count <= limit;
        } catch (Exception e) {
            log.debug("Redis rate limiter unavailable ({}), falling back to memory", e.getMessage());
            return checkInMemory(key, limit);
        }
    }

    private boolean checkInMemory(String key, int limit) {
        long now = System.currentTimeMillis();
        CounterWindow window = memoryCounters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startTime > 60_000) {
                return new CounterWindow(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return window.count.get() <= limit;
    }

    private static class CounterWindow {
        final long startTime;
        final AtomicInteger count;

        CounterWindow(long startTime, AtomicInteger count) {
            this.startTime = startTime;
            this.count = count;
        }
    }
}
