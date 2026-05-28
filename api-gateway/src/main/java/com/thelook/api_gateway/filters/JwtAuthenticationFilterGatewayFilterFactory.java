package com.thelook.api_gateway.filters;

import com.thelook.api_gateway.security.JwtService;
import com.thelook.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilterGatewayFilterFactory.Config>
        implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilterGatewayFilterFactory.class);

    private final JwtService jwtService;
    private final ReactiveRedisOperations<String, String> redisOps;
    private final GatewaySecurityProperties securityProperties;

    @org.springframework.beans.factory.annotation.Value("${internal.secret}")
    private String internalSecret;

    public JwtAuthenticationFilterGatewayFilterFactory(JwtService jwtService,
                                                       @Qualifier("reactiveRedisOperations")
                                                       ReactiveRedisOperations<String, String> redisOps,
                                                       GatewaySecurityProperties securityProperties) {
        super(Config.class);
        this.jwtService = jwtService;
        this.redisOps = redisOps;
        this.securityProperties = securityProperties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (isPublicPath(path)) {
                ServerHttpRequest publicRequest = exchange.getRequest().mutate()
                        .header("X-Internal-Secret", internalSecret)
                        .build();
                return chain.filter(exchange.mutate().request(publicRequest).build());
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

                return redisOps.hasKey("auth:blocklist:" + token)
                        .flatMap(isBlocked -> {
                            if (Boolean.TRUE.equals(isBlocked)) {
                                return Mono.error(new UnauthorizedException("Token inválido ou expirado"));
                            }

                            if (creatorIdFromJwt != null) {
                                return proceedWithHeaders(exchange, chain::filter, userId, username, role, creatorIdFromJwt);
                            }

                            return redisOps.opsForValue().get("user:profile:" + userId)
                                    .flatMap(creatorIdFromRedis ->
                                            proceedWithHeaders(exchange, chain::filter, userId, username, role, creatorIdFromRedis))
                                    .switchIfEmpty(Mono.defer(() ->
                                            proceedWithHeaders(exchange, chain::filter, userId, username, role, null)));
                        })
                        .onErrorResume(UnauthorizedException.class, Mono::error)
                        .onErrorResume(e -> {
                            log.error("Erro inesperado no filtro JWT: {}", e.getMessage(), e);
                            return Mono.error(new UnauthorizedException("Token inválido ou expirado"));
                        });

            } catch (Exception e) {
                log.warn("Token validation failed: {}", e.getMessage());
                return Mono.error(new UnauthorizedException("Token inválido ou expirado"));
            }
        };
    }

    private boolean isPublicPath(String path) {
        return securityProperties.getPublicPaths().stream().anyMatch(path::startsWith);
    }

    private Mono<Void> proceedWithHeaders(ServerWebExchange exchange,
                                          java.util.function.Function<ServerWebExchange, Mono<Void>> next,
                                          String userId,
                                          String username,
                                          String role,
                                          String creatorId) {

        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header("X-Internal-Secret", internalSecret)
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .header("X-User-Role", role);

        if (creatorId != null) {
            builder.header("X-Creator-Id", creatorId);
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        var securityContext = new SecurityContextImpl(authentication);

        return next.apply(exchange.mutate().request(builder.build()).build())
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
    }

    @Override
    public int getOrder() {
        return -2;
    }

    public static class Config {
    }
}