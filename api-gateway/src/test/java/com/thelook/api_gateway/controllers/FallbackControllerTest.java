package com.thelook.api_gateway.controllers;

import com.thelook.exceptions.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackControllerTest {

    private FallbackController controller;

    @BeforeEach
    void setUp() {
        controller = new FallbackController();
    }

    @Test
    void fallback_withExceptionMessageHeader_throwsWithCause() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/fallback/auth")
                        .header("X-Exception-Message", "Connection refused")
                        .build()
        );

        assertThatThrownBy(() -> controller.fallback("auth", exchange))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("auth")
                .hasMessageContaining("Connection refused");
    }

    @Test
    void fallback_withoutExceptionMessageHeader_throwsWithNullCause() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/fallback/social").build()
        );

        assertThatThrownBy(() -> controller.fallback("social", exchange))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("social");
    }

    @Test
    void fallback_differentServiceNames_includesServiceNameInMessage() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/fallback/ms-feed").build()
        );

        assertThatThrownBy(() -> controller.fallback("ms-feed", exchange))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("ms-feed");
    }
}