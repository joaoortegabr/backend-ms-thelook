package com.thelook.ms_auth.models.dtos;

import com.thelook.ms_auth.models.enums.UserRole;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String password,
        UserRole role
    ) {

}
