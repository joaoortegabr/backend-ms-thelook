package com.thelook.ms_social.models.dtos;

import java.time.LocalDate;
import java.util.UUID;

public record CreatorResponse(
        UUID id,
        String name,
        String avatarUrl,
        String bio,
        String instagram,
        LocalDate birthDate,
        String city,
        String uf
    ) {
}
