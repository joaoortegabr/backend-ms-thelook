package com.thelook.dtos;

import java.util.UUID;

public record CreatorLifecycleEventDTO(String type, UUID creatorId, UUID userId) {}
