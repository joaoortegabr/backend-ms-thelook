package com.thelook.ms_auth.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
    ) {
}
