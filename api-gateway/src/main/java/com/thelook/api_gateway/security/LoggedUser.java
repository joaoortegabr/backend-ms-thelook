package com.thelook.api_gateway.security;

import com.thelook.api_gateway.dtos.UserContext;
import com.thelook.api_gateway.enums.UserRole;

import java.util.UUID;

/**
 * Utilitário de contexto de usuário para uso EXCLUSIVO em código servlet (não-reativo).
 * NÃO usar em filtros ou handlers WebFlux — o contexto deve ser propagado via
 * ServerWebExchange.getAttributes() ou ReactiveSecurityContextHolder.
 * Sempre chamar remove() em bloco finally após o uso.
 */
public class LoggedUser {

    private static final ThreadLocal<UserContext> context = new ThreadLocal<>();

    public static void set(UserContext user) {
        context.set(user);
    }

    public static UserContext get() {
        return context.get();
    }

    public static void remove() {
        context.remove();
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
