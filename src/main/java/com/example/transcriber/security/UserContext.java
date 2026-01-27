package com.example.transcriber.security;

import java.util.List;

public class UserContext {
    private static final ThreadLocal<Long> userId = new ThreadLocal<>();
    private static final ThreadLocal<List<String>> permissions = new ThreadLocal<>();

    public static void setUserId(Long id) {
        userId.set(id);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void setPermissions(List<String> perms) {
        permissions.set(perms);
    }

    public static List<String> getPermissions() {
        return permissions.get();
    }

    public static void clear() {
        userId.remove();
        permissions.remove();
    }
}
