package com.thelook.api_gateway.config;

import com.thelook.api_gateway.filters.GatewaySecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final GatewaySecurityProperties securityProperties;

    public SecurityConfig(GatewaySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        String[] publicPaths = securityProperties.getPublicPaths().toArray(String[]::new);

        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::deny)
                        .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubdomains(true)
                                .maxAge(Duration.ofDays(365)))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(publicPaths).permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, e) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                );
        return http.build();
    }
}