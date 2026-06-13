package com.itheima.smartcodenote.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.security.CurrentUser;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                    return RateLimiter.create(10.0 / 60.0); // default 10/min
                }
            });

    @Around("@annotation(rateLimit)")
    public Object check(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long userId = CurrentUser.getUserId();
        if (userId == null) {
            // Allow unauthenticated requests through (login/register)
            return joinPoint.proceed();
        }

        // Get or create per-user rate limiter with configured rate
        double permitsPerSecond = rateLimit.permits()
                / (double) rateLimit.unit().toSeconds(rateLimit.duration());
        RateLimiter limiter;
        try {
            limiter = userLimiters.get(userId);
        } catch (ExecutionException e) {
            limiter = RateLimiter.create(permitsPerSecond);
        }
        limiter.setRate(permitsPerSecond);

        if (!limiter.tryAcquire()) {
            log.warn("Rate limit exceeded: user={}, method={}, permits/sec={}",
                    userId, joinPoint.getSignature().toShortString(), permitsPerSecond);
            throw new BusinessException(429, rateLimit.message());
        }

        return joinPoint.proceed();
    }
}
