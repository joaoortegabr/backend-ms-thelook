package com.thelook.ms_social.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

public record CreatorRequest(
        @NotBlank @Size(max = 64) String name,
        @URL String avatarUrl,
        @Size(max = 255) String bio,
        @NotBlank @Size(max = 32) String instagram,
        LocalDate birthDate,
        @Size(max = 64) String city,
        @Size(min = 2, max = 2) String uf
    ) {
}