package com.thelook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class InternalSecretFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Internal-Secret";

    private final byte[] expectedSecretBytes;

    public InternalSecretFilter(String expectedSecret) {
        this.expectedSecretBytes = expectedSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        if (received == null || !MessageDigest.isEqual(
                expectedSecretBytes, received.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Direct access to internal services is not allowed\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
