package com.thelook.api_gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");

        if (userId != null) {
            exchange.getAttributes().put("userId", userId);
        }

        return chain.filter(exchange);
    }
}