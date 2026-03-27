package com.thelook.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class GatewayConfig {

//    @Bean
//    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
//        return builder.routes()
//                .route("ms-auth-public", r -> r.path("/api/v1/auth/**")
//                        .uri("lb://ms-auth"))
//
//                .route("ms-creation-secure", r -> r.path("/api/v1/creation/**")
//                        .uri("lb://ms-creation"))
//
//                .route("ms-social-secure", r -> r.path("/api/v1/social/**")
//                        .uri("lb://ms-social"))
//
//                .build();
//    }

}