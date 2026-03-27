package com.thelook.ms_auth.models.dtos;

import com.thelook.ms_auth.models.enums.UserRole;

public record LoginRequest(
        String username,
        String password
    ) {

}
