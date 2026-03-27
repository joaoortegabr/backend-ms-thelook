package com.thelook.api_gateway.security;

import com.thelook.api_gateway.dtos.UserContext;
import com.thelook.api_gateway.enums.UserRole;

import java.util.UUID;

public class LoggedUser {

    private static final ThreadLocal<UserContext> context = new ThreadLocal<>();

    public static void set(UserContext user) {
        context.set(user);
    }

    public static UserContext get() {
        return context.get();
    }

    public static UUID getUserId() {
        return context.get().userId();
    }

    public static String getUsername() {
        return context.get().username();
    }

    public static UserRole getUserRole() {
        return context.get().role();
    }

}
