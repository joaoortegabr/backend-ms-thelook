package com.thelook.security;

public class UserContext {
    private static final ThreadLocal<String> CURRENT_CREATOR = new ThreadLocal<>();

    public static void setCreatorId(String id) {
        CURRENT_CREATOR.set(id);
    }

    public static String getCreatorId() {
        return CURRENT_CREATOR.get();
    }

    public static void clear() {
        CURRENT_CREATOR.remove();
    }
}