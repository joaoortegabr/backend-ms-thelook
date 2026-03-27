package com.thelook.ms_auth.models.dtos;

public record UserRequest(
        String username,
        String password
    ) {

}
