package com.thelook.ms_auth.models.dtos;

public record LoginResponse(
        String token,
        String type,
        long expiresIn
    ) {

}
