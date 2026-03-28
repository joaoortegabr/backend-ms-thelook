package com.thelook.api_gateway.filters;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
@Component
public class GatewaySecurityProperties {

    private final List<String> publicPaths = new ArrayList<>();

    public List<String> getPublicPaths() { return publicPaths; }
}
