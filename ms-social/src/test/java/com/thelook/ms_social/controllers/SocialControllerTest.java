package com.thelook.ms_social.controllers;

import com.thelook.ms_social.config.SecurityConfig;
import com.thelook.ms_social.services.SocialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SocialController.class)
@Import(SecurityConfig.class)
class SocialControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SocialService socialService;

    // =========================================================
    // PATCH /follow/{targetId}
    // =========================================================

    @Test
    void follow_withCreatorIdHeader_returns200AndCallsService() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        doNothing().when(socialService).follow(creatorId, targetId);

        mockMvc.perform(patch("/api/v1/social/follow/{targetId}", targetId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isOk());

        verify(socialService).follow(creatorId, targetId);
    }

    @Test
    void follow_missingCreatorIdHeader_doesNotCallServiceAndReturnsError() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/social/follow/{targetId}", targetId))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(200));

        verifyNoInteractions(socialService);
    }

    @Test
    void follow_serviceThrowsBusinessRule_propagatesError() throws Exception {
        UUID creatorId = UUID.randomUUID();
        doThrow(new com.thelook.exceptions.BusinessRuleException("can't follow yourself"))
                .when(socialService).follow(creatorId, creatorId);

        mockMvc.perform(patch("/api/v1/social/follow/{targetId}", creatorId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(200));
    }

    // =========================================================
    // PATCH /unfollow/{targetId}
    // =========================================================

    @Test
    void unfollow_withCreatorIdHeader_returns200AndCallsService() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        doNothing().when(socialService).unfollow(creatorId, targetId);

        mockMvc.perform(patch("/api/v1/social/unfollow/{targetId}", targetId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isOk());

        verify(socialService).unfollow(creatorId, targetId);
    }

    @Test
    void unfollow_missingCreatorIdHeader_doesNotCallServiceAndReturnsError() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/social/unfollow/{targetId}", targetId))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(200));

        verifyNoInteractions(socialService);
    }
}