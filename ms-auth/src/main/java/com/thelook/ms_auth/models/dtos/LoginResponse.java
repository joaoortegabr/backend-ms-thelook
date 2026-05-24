package com.thelook.ms_auth.models.dtos;

public record LoginResponse(
        String token,
        String refreshToken,
        String type,
        long expiresIn,
        long refreshExpiresIn
    ) {

}

