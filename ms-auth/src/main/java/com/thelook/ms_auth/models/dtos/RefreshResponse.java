package com.thelook.ms_auth.models.dtos;

public record RefreshResponse(
        String token,
        String type,
        long expiresIn
) {
}