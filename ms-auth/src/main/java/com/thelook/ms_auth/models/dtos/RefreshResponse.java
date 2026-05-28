package com.thelook.ms_auth.models.dtos;

public record RefreshResponse(
        String token,
        String refreshToken,
        String type,
        long expiresIn,
        long refreshExpiresIn
) {}