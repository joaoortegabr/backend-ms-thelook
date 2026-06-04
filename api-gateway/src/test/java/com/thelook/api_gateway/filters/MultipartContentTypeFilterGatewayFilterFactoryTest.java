package com.thelook.api_gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultipartContentTypeFilterGatewayFilterFactoryTest {

    @Mock
    private GatewayFilterChain chain;

    private GatewayFilter gatewayFilter;

    @BeforeEach
    void setUp() {
        MultipartContentTypeFilterGatewayFilterFactory factory =
                new MultipartContentTypeFilterGatewayFilterFactory();
        gatewayFilter = factory.apply(new MultipartContentTypeFilterGatewayFilterFactory.Config());
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // =========================================================
    // POST with multipart/form-data — passes through
    // =========================================================

    @Test
    void filter_postWithMultipart_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/creation/outfits")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================
    // POST with JSON — 415
    // =========================================================

    @Test
    void filter_postWithJson_returns415() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/creation/outfits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================
    // POST with no Content-Type — 415
    // =========================================================

    @Test
    void filter_postWithNullContentType_returns415() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/creation/outfits")
                        .build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================
    // GET — passes through without content-type check
    // =========================================================

    @Test
    void filter_getRequest_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/creation/outfits/123").build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================
    // DELETE — passes through without content-type check
    // =========================================================

    @Test
    void filter_deleteRequest_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.delete("/api/v1/creation/outfits/123").build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // =========================================================
    // PUT with JSON — 415 (body-carrying method, not multipart)
    // =========================================================

    @Test
    void filter_putWithJson_returns415() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.put("/api/v1/creation/outfits/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .build());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
                .verifyComplete();

        verify(chain, never()).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}