package com.sso.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Simple per-IP rate limiter for payment endpoints.
 * Allows MAX_REQUESTS calls per IP within WINDOW_SECONDS.
 */
@Service
@RequiredArgsConstructor
public class PaymentRateLimitService {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60L;

    private final StringRedisTemplate redisTemplate;

    /**
     * Returns true if the request is allowed, false if the rate limit is exceeded.
     */
    public boolean isAllowed(String clientIp) {
        String key = "rate:payment:" + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
        }
        return count == null || count <= MAX_REQUESTS;
    }
}
