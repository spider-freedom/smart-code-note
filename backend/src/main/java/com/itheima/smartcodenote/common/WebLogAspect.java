package com.itheima.smartcodenote.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * Unified request logging via AOP.
 *
 * Logs every controller request with method, URI, args (safe), elapsed time.
 * Flags slow requests (> 3s) with WARN level for performance monitoring.
 */
@Aspect
@Component
public class WebLogAspect {

    private static final Logger log = LoggerFactory.getLogger(WebLogAspect.class);
    private static final long SLOW_THRESHOLD_MS = 3000;

    @Pointcut("execution(* com.itheima.smartcodenote.controller..*(..))")
    public void controllerLayer() {}

    @Around("controllerLayer()")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        String uri = getRequestUri();

        // Safe args — skip large binary content (MultipartFile) and request body objects
        String safeArgs = safeStringify(joinPoint.getArgs());

        log.info("[REQ] {} {} | args={}", method, uri, safeArgs);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed > SLOW_THRESHOLD_MS) {
                log.warn("[SLOW] {} {} | {}ms (threshold {}ms)",
                        method, uri, elapsed, SLOW_THRESHOLD_MS);
            } else {
                log.info("[RESP] {} {} | {}ms", method, uri, elapsed);
            }

            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[ERROR] {} {} | {}ms | {}: {}",
                    method, uri, elapsed,
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private String getRequestUri() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getMethod() + " " + request.getRequestURI();
            }
        } catch (Exception ignored) {}
        return "N/A";
    }

    private String safeStringify(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof MultipartFile f) {
                        return "MultipartFile(" + f.getOriginalFilename() + ", " + f.getSize() + " bytes)";
                    }
                    if (arg instanceof jakarta.servlet.http.HttpServletRequest
                            || arg instanceof jakarta.servlet.http.HttpServletResponse) {
                        return arg.getClass().getSimpleName();
                    }
                    return String.valueOf(arg);
                })
                .toList()
                .toString();
    }
}
