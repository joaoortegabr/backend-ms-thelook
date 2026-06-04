package com.thelook.api_gateway.filters;

import com.thelook.api_gateway.security.JwtService;
import com.thelook.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterGatewayFilterFactoryTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ReactiveRedisOperations<String, String> redisOps;

    @Mock
    private ReactiveValueOperations<String, String> valueOps;

    @Mock
    private GatewaySecurityProperties securityProperties;

    @Mock
    private GatewayFilterChain chain;

    private JwtAuthenticationFilterGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthenticationFilterGatewayFilterFactory(jwtService, redisOps, securityProperties);
        when(securityProperties.getPublicPaths()).thenReturn(
                List.of("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh")
        );
    }

    private ServerWebExchange exchange(String path, String authHeader) {
        var builder = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private GatewayFilter filter() {
        return filterFactory.apply(new JwtAuthenticationFilterGatewayFilterFactory.Config());
    }

    // --- Public paths ---

    @Test
    void publicPath_noToken_passesThrough() {
        ServerWebExchange ex = exchange("/api/v1/auth/login", null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        verify(chain).filter(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    void publicPath_refreshEndpoint_passesThrough() {
        ServerWebExchange ex = exchange("/api/v1/auth/refresh", null);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        verifyNoInteractions(jwtService);
    }

    // --- Missing / malformed Authorization header ---

    @Test
    void missingAuthHeader_returnsUnauthorized() {
        ServerWebExchange ex = exchange("/api/v1/creators/123", null);

        StepVerifier.create(filter().filter(ex, chain))
                .expectError(UnauthorizedException.class)
                .verify();

        verifyNoInteractions(jwtService);
    }

    @Test
    void basicAuthHeader_returnsUnauthorized() {
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Basic dXNlcjpwYXNz");

        StepVerifier.create(filter().filter(ex, chain))
                .expectError(UnauthorizedException.class)
                .verify();

        verifyNoInteractions(jwtService);
    }

    // --- Blocklist ---

    @Test
    void blockedToken_returnsUnauthorized() {
        String token = "some.valid.token";
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
        when(jwtService.extractUsername(token)).thenReturn("user1");
        when(jwtService.extractRole(token)).thenReturn("CREATOR");
        when(jwtService.extractCreatorId(token)).thenReturn(null);
        when(redisOps.hasKey("auth:blocklist:" + token)).thenReturn(Mono.just(true));

        StepVerifier.create(filter().filter(ex, chain))
                .expectError(UnauthorizedException.class)
                .verify();

        verifyNoInteractions(chain);
    }

    // --- Valid token: creatorId in JWT ---

    @Test
    void validToken_creatorIdInJwt_proceedsWithAllHeaders() {
        String token = "some.valid.token";
        String userId = UUID.randomUUID().toString();
        String creatorId = UUID.randomUUID().toString();
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractUsername(token)).thenReturn("user1");
        when(jwtService.extractRole(token)).thenReturn("CREATOR");
        when(jwtService.extractCreatorId(token)).thenReturn(creatorId);
        when(redisOps.hasKey("auth:blocklist:" + token)).thenReturn(Mono.just(false));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        HttpHeaders headers = captor.getValue().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo(userId);
        assertThat(headers.getFirst("X-Username")).isEqualTo("user1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("CREATOR");
        assertThat(headers.getFirst("X-Creator-Id")).isEqualTo(creatorId);
    }

    // --- Valid token: creatorId from Redis ---

    @Test
    void validToken_creatorIdInRedis_proceedsWithCreatorIdHeader() {
        String token = "some.valid.token";
        String userId = UUID.randomUUID().toString();
        String creatorId = UUID.randomUUID().toString();
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractUsername(token)).thenReturn("user1");
        when(jwtService.extractRole(token)).thenReturn("CREATOR");
        when(jwtService.extractCreatorId(token)).thenReturn(null);
        when(redisOps.hasKey("auth:blocklist:" + token)).thenReturn(Mono.just(false));
        when(redisOps.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("user:profile:" + userId)).thenReturn(Mono.just(creatorId));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Creator-Id")).isEqualTo(creatorId);
    }

    // --- Valid token: no creatorId anywhere ---

    @Test
    void validToken_noCreatorIdAnywhere_proceedsWithoutCreatorIdHeader() {
        String token = "some.valid.token";
        String userId = UUID.randomUUID().toString();
        ServerWebExchange ex = exchange("/api/v1/outfits/feed", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractUsername(token)).thenReturn("user1");
        when(jwtService.extractRole(token)).thenReturn("CREATOR");
        when(jwtService.extractCreatorId(token)).thenReturn(null);
        when(redisOps.hasKey("auth:blocklist:" + token)).thenReturn(Mono.just(false));
        when(redisOps.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("user:profile:" + userId)).thenReturn(Mono.empty());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-Creator-Id")).isNull();
    }

    // --- Invalid JWT ---

    @Test
    void invalidToken_jwtServiceThrows_returnsUnauthorized() {
        String token = "invalid.token";
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenThrow(new RuntimeException("Bad token"));

        StepVerifier.create(filter().filter(ex, chain))
                .expectError(UnauthorizedException.class)
                .verify();

        verifyNoInteractions(chain);
    }

    // --- Admin role ---

    @Test
    void validToken_adminRole_propagatesRoleHeader() {
        String token = "admin.token";
        String userId = UUID.randomUUID().toString();
        ServerWebExchange ex = exchange("/api/v1/creators/123", "Bearer " + token);

        when(jwtService.extractUserId(token)).thenReturn(userId);
        when(jwtService.extractUsername(token)).thenReturn("admin");
        when(jwtService.extractRole(token)).thenReturn("ADMIN");
        when(jwtService.extractCreatorId(token)).thenReturn(null);
        when(redisOps.hasKey("auth:blocklist:" + token)).thenReturn(Mono.just(false));
        when(redisOps.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("user:profile:" + userId)).thenReturn(Mono.empty());
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter().filter(ex, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
    }
}