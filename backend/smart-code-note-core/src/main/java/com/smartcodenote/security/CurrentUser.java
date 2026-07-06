package com.smartcodenote.security;

import com.smartcodenote.exception.BusinessException;

public final class CurrentUser {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BusinessException(401, "unauthorized");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
