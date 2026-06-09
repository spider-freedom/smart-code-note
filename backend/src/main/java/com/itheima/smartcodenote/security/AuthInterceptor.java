package com.itheima.smartcodenote.security;

import com.itheima.smartcodenote.exception.BusinessException;
import com.itheima.smartcodenote.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = null;
        String header = request.getHeader(AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            token = header.substring(BEARER_PREFIX.length());
        }
        if (token == null) {
            token = request.getParameter("token");
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "unauthorized");
        }
        Long userId = jwtUtil.verifyAndGetUserId(token);
        CurrentUser.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
