package com.thelook.ms_social.models.dtos;

public record CreatorUpdateRequest(
        String avatarUrl,
        String bio,
        String instagram,
        String city,
        String uf
    ) {
}
