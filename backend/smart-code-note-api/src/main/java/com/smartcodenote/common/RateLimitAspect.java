package com.smartcodenote.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.smartcodenote.exception.BusinessException;
import com.smartcodenote.security.CurrentUser;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AOP rate limiter using Guava RateLimiter (per-user, token-bucket algorithm).
 *
 * Design notes:
 * - Uses LoadingCache to lazily create per-user RateLimiter instances.
 * - Token bucket algorithm: permits regenerated at a steady rate.
 * - Non-blocking tryAcquire(0): returns false immediately when no permit available.
 * - Production: replace with Redis + Lua script for distributed rate limiting
 *   across multiple service instances.
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "smart-code-note.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /**
     * Per-user rate limiter cache. Key = userId, Value = RateLimiter.
     * Set 1h expiry to clean up stale entries for inactive users.
     */
    private final LoadingCache<Long, RateLimiter> userLimiters = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public RateLimiter load(Long userId) {
                    return RateLimiter.create(10.0 / 60.0);
                }
            });

    /**
     * Track whether a user has had at least one successful request through.
     * First request is always allowed so slow-rate limiters don't block the initial call.
     */
    private final ConcurrentHashMap<Long, Boolean> firstRequestGranted = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object check(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            return joinPoint.proceed();
        }

        double permitsPerSecond = rateLimit.permits()
                / (double) rateLimit.unit().toSeconds(rateLimit.duration());
        RateLimiter limiter;
        try {
            limiter = userLimiters.get(userId);
            if (Math.abs(limiter.getRate() - permitsPerSecond) > 0.001) {
                limiter.setRate(permitsPerSecond);
            }
        } catch (ExecutionException e) {
            limiter = RateLimiter.create(permitsPerSecond);
        }

        // First request per user always passes (prevents cold-start blocking)
        if (firstRequestGranted.putIfAbsent(userId, Boolean.TRUE) == null) {
            limiter.tryAcquire(); // consume the permit without blocking
            return joinPoint.proceed();
        }

        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded: user={}, method={}, permits/sec={}",
                    userId, joinPoint.getSignature().toShortString(), permitsPerSecond);
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }
}
