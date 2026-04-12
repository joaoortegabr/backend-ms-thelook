package com.thelook.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class CreatorContextFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String creatorId = request.getHeader("X-Creator-Id");
        if (creatorId != null) {
            UserContext.setCreatorId(creatorId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
