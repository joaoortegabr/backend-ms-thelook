package com.thelook;

import com.thelook.exceptions.UnauthorizedException;
import com.thelook.exceptions.ServiceUnavailableException;
import com.thelook.exceptions.StandardError;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@AutoConfiguration
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<StandardError>> handleUnauthorized(UnauthorizedException e, ServerWebExchange exchange) {

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        HttpStatus status = HttpStatus.UNAUTHORIZED;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Invalid or expired token",
                e.getMessage(),
                path,
                correlationId != null ? correlationId : "N/A"
        );

        return Mono.just(ResponseEntity.status(status).body(err));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public Mono<ResponseEntity<StandardError>> handleUnavailableServiceException(ServiceUnavailableException e, ServerWebExchange exchange) {

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Service unavailable",
                e.getMessage(),
                path,
                correlationId != null ? correlationId : "N/A"
        );

        return Mono.just(ResponseEntity.status(status).body(err));
    }


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<StandardError>> handleGeneralExceptions(Exception e, ServerWebExchange exchange) {

        String path = exchange.getRequest().getURI().getPath();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        StandardError err = new StandardError(
                Instant.now(),
                status.value(),
                "Unknown error",
                e.getMessage(),
                path,
                correlationId != null ? correlationId : "N/A"
        );

        return Mono.just(ResponseEntity.status(status).body(err));
    }
}
