package com.thelook.ms_social.models.dtos;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreatorUpdateRequest(
        @URL String avatarUrl,
        @Size(max = 255) String bio,
        @Size(max = 64) String instagram,
        @Size(max = 128) String city,
        @Size(min = 2, max = 2) String uf
    ) {
}