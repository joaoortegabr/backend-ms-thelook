package com.thelook.api_gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class MultipartContentTypeFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<MultipartContentTypeFilterGatewayFilterFactory.Config> {

    public MultipartContentTypeFilterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpMethod method = exchange.getRequest().getMethod();

            // GET, DELETE, HEAD não carregam body — validação não se aplica
            if (method == HttpMethod.GET || method == HttpMethod.DELETE || method == HttpMethod.HEAD) {
                return chain.filter(exchange);
            }

            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType == null || !contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {}
}