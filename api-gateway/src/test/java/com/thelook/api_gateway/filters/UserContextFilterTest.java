package com.thelook.api_gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserContextFilterTest {

    @Mock
    private GatewayFilterChain chain;

    private UserContextFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserContextFilter();
    }

    @Test
    void filter_withXUserIdHeader_storesUserIdInAttributes() {
        String userId = "abc-123";
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/creators/1")
                        .header("X-User-Id", userId)
                        .build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getAttributes().get("userId")).isEqualTo(userId);
        verify(chain).filter(exchange);
    }

    @Test
    void filter_withoutXUserIdHeader_doesNotSetAttribute() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/outfits/feed").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getAttributes().get("userId")).isNull();
        verify(chain).filter(exchange);
    }

    @Test
    void filter_alwaysContinuesChain() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/outfits/feed").build()
        );
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }
}