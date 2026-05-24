package com.thelook.ms_social.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreatorRequest(
        @NotBlank String name,
        String avatarUrl,
        String bio,
        @NotBlank String instagram,
        LocalDate birthDate,
        String city,
        @Size(min = 2, max = 2) String uf
    ) {
}
