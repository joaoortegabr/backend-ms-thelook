package com.thelook.api_gateway.controllers;

import com.thelook.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback/{service}")
    public void fallback(@PathVariable String service, ServerWebExchange exchange) {
        log.info("/////Fallback activated={}", service);

        String cause = exchange.getRequest().getHeaders().getFirst("X-Exception-Message");
        throw new ServiceUnavailableException(service + " service unavailable: " + cause);
    }

}
