package com.thelook.ms_social.models.dtos;

import org.hibernate.validator.constraints.UUID;

public record CreatorFollowerCount(
        String creatorId,
        long total
    ) {
}
