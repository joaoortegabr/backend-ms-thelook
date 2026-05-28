package com.thelook.ms_auth.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @Size(min = 8, max = 72, message = "Password must be 8–72 characters")
        @Pattern(regexp = ".*[A-Z].*", message = "Password must contain an uppercase letter")
        @NotBlank String password
    ) {
}
