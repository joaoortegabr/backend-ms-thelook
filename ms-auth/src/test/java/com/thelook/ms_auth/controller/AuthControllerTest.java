package com.thelook.ms_auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.InvalidAccessTokenException;
import com.thelook.exceptions.UnprocessableRequestException;
import com.thelook.ms_auth.config.SecurityConfig;
import com.thelook.ms_auth.models.dtos.*;
import com.thelook.ms_auth.services.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean AuthService authService;

    // =========================================================
    // POST /register
    // =========================================================

    @Test
    void register_validRequest_returns201WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.register(any())).thenReturn(new RegisterResponse(id, "alice"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass123\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void register_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_duplicateUsername_returns4xx() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessRuleException("Username already registered"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("alice", "pass"))))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // POST /login
    // =========================================================

    @Test
    void login_validRequest_returns200WithTokens() throws Exception {
        LoginResponse loginResponse = new LoginResponse("access.token", "refresh.token", "Bearer", 900_000L, 7_200_000L);
        when(authService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900_000))
                .andExpect(jsonPath("$.refreshExpiresIn").value(7_200_000));
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_invalidCredentials_returns4xx() throws Exception {
        when(authService.login(any())).thenThrow(new UnprocessableRequestException("Invalid username or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "wrong"))))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // POST /refresh
    // =========================================================

    @Test
    void refresh_validRequest_returns200WithNewToken() throws Exception {
        RefreshResponse refreshResponse = new RefreshResponse("new.access.token", "Bearer", 900_000L);
        when(authService.refresh(anyString())).thenReturn(refreshResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("valid.refresh.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new.access.token"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900_000));
    }

    @Test
    void refresh_blankRefreshToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void refresh_invalidRefreshToken_returns4xx() throws Exception {
        when(authService.refresh(anyString())).thenThrow(new InvalidAccessTokenException("Refresh token is invalid or expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("bad.refresh.token"))))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // POST /logout
    // =========================================================

    @Test
    void logout_validAuthHeader_returns204() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer some.valid.token"))
                .andExpect(status().isNoContent());

        verify(authService).logout("Bearer some.valid.token");
    }

    @Test
    void logout_invalidToken_returns4xx() throws Exception {
        doThrow(new InvalidAccessTokenException("Token is invalid or already expired"))
                .when(authService).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void logout_missingAuthHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().is4xxClientError());
    }
}