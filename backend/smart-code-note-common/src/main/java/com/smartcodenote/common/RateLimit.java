package com.smartcodenote.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Rate-limit annotation for controller methods.
 *
 * Default: 10 permits per minute, per user.
 * Production would use Redis + Lua for distributed rate limiting.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Permits per time window (default 10) */
    double permits() default 10.0;

    /** Time window duration (default 1 minute) */
    long duration() default 1;

    /** Time unit for duration (default MINUTES) */
    TimeUnit unit() default TimeUnit.MINUTES;

    /** Message returned when rate limit exceeded */
    String message() default "请求过于频繁，请稍后再试";
}
