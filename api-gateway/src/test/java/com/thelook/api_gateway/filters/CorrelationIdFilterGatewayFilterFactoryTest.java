package com.thelook.api_gateway.filters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterGatewayFilterFactoryTest {

    @Mock
    private GatewayFilterChain chain;

    private CorrelationIdFilterGatewayFilterFactory filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilterGatewayFilterFactory();
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // =========================================================
    // Header already present — preserved as-is
    // =========================================================

    @Test
    void filter_correlationIdPresent_preservesExistingHeader() {
        String existingId = "existing-correlation-id-123";
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/test")
                .header("X-Correlation-ID", existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        String forwarded = captor.getValue().getRequest().getHeaders().getFirst("X-Correlation-ID");
        assertThat(forwarded).isEqualTo(existingId);
    }

    // =========================================================
    // Header absent — UUID generated
    // =========================================================

    @Test
    void filter_correlationIdAbsent_generatesUuidHeader() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/test")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        String generated = captor.getValue().getRequest().getHeaders().getFirst("X-Correlation-ID");
        assertThat(generated).isNotNull().isNotBlank();
        // Must be a valid UUID format
        assertThat(generated).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    void filter_correlationIdAbsent_eachCallGeneratesDifferentId() {
        MockServerWebExchange ex1 = MockServerWebExchange.from(MockServerHttpRequest.get("/a").build());
        MockServerWebExchange ex2 = MockServerWebExchange.from(MockServerHttpRequest.get("/b").build());

        ArgumentCaptor<ServerWebExchange> cap1 = ArgumentCaptor.forClass(ServerWebExchange.class);
        ArgumentCaptor<ServerWebExchange> cap2 = ArgumentCaptor.forClass(ServerWebExchange.class);

        StepVerifier.create(filter.filter(ex1, chain)).verifyComplete();
        verify(chain).filter(cap1.capture());

        StepVerifier.create(filter.filter(ex2, chain)).verifyComplete();
        verify(chain, org.mockito.Mockito.times(2)).filter(cap2.capture());

        String id1 = cap1.getValue().getRequest().getHeaders().getFirst("X-Correlation-ID");
        String id2 = cap2.getValue().getRequest().getHeaders().getFirst("X-Correlation-ID");
        assertThat(id1).isNotEqualTo(id2);
    }

    // =========================================================
    // Chain always called
    // =========================================================

    @Test
    void filter_alwaysCallsChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(any());
    }

    // =========================================================
    // Order
    // =========================================================

    @Test
    void getOrder_returnsMinus3() {
        assertThat(filter.getOrder()).isEqualTo(-3);
    }
}