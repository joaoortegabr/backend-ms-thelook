package com.thelook.ms_auth.models.dtos;

public record RegisterRequest(
        String username,
        String password
    ) {
}
