package com.thelook.api_gateway.filters;

import com.thelook.api_gateway.security.JwtService;
import com.thelook.exceptions.UnauthorizedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilterGatewayFilterFactory.Config>
        implements Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/auth/login",
            "/eureka"
    );

    private final JwtService jwtService;
    private final ReactiveRedisOperations<String, String> redisOps;

    public JwtAuthenticationFilterGatewayFilterFactory(JwtService jwtService,
                                                       @Qualifier("reactiveRedisOperations")
                                                       ReactiveRedisOperations<String, String> redisOps) {
        super(Config.class);
        this.jwtService = jwtService;
        this.redisOps = redisOps;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Mono.error(new UnauthorizedException("Token ausente"));
            }

            try {
                String token = authHeader.substring(7);

                String userId = jwtService.extractUserId(token);
                String username = jwtService.extractUsername(token);
                String role = jwtService.extractRole(token);
                String creatorIdFromJwt = jwtService.extractCreatorId(token);

                if (creatorIdFromJwt != null) {
                    return proceedWithHeaders(exchange, chain::filter, userId, username, role, creatorIdFromJwt);
                }

                return redisOps.opsForValue().get("user:profile:" + userId)
                        .flatMap(creatorIdFromRedis ->
                                proceedWithHeaders(exchange, chain::filter, userId, username, role, creatorIdFromRedis))
                        .switchIfEmpty(Mono.defer(() ->
                                proceedWithHeaders(exchange, chain::filter, userId, username, role, null)));

            } catch (Exception e) {
                return Mono.error(new UnauthorizedException("Token inválido ou expirado"));
            }
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> proceedWithHeaders(ServerWebExchange exchange,
                                          java.util.function.Function<ServerWebExchange, Mono<Void>> next,
                                          String userId,
                                          String username,
                                          String role,
                                          String creatorId) {

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role);

        if (creatorId != null) {
            builder.header("X-Creator-Id", creatorId);
        }

        return next.apply(exchange.mutate().request(builder.build()).build());
    }

    @Override
    public int getOrder() {
        return -2;
    }

    public static class Config {
    }
}