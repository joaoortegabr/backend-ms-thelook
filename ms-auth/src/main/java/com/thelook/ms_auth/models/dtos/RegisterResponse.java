package com.thelook.ms_auth.models.dtos;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String username
    ) {
}
