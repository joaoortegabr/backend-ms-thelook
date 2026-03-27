package com.thelook.api_gateway.dtos;

import com.thelook.api_gateway.enums.UserRole;

import java.util.UUID;

public record UserContext(
    UUID userId,
    String username,
    UserRole role
    ) {
}
