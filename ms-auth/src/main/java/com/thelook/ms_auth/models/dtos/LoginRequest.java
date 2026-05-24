package com.thelook.ms_auth.models.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {

}
