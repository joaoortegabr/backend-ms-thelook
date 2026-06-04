package com.thelook.ms_social.controllers;

import com.thelook.GlobalExceptionHandler;
import com.thelook.ms_social.services.SocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SocialControllerTest {

    @Mock SocialService socialService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SocialController controller = new SocialController(socialService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================
    // POST /follow/{targetId}
    // =========================================================

    @Test
    void follow_withCreatorIdHeader_returns200AndCallsService() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        doNothing().when(socialService).follow(creatorId, targetId);

        mockMvc.perform(post("/api/v1/social/follow/{targetId}", targetId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isOk());

        verify(socialService).follow(creatorId, targetId);
    }

    @Test
    void follow_missingCreatorIdHeader_doesNotCallServiceAndReturnsError() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/social/follow/{targetId}", targetId))
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

        mockMvc.perform(post("/api/v1/social/follow/{targetId}", creatorId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(200));
    }

    // =========================================================
    // DELETE /follow/{targetId}
    // =========================================================

    @Test
    void unfollow_withCreatorIdHeader_returns204AndCallsService() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        doNothing().when(socialService).unfollow(creatorId, targetId);

        mockMvc.perform(delete("/api/v1/social/follow/{targetId}", targetId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isNoContent());

        verify(socialService).unfollow(creatorId, targetId);
    }

    @Test
    void unfollow_missingCreatorIdHeader_doesNotCallServiceAndReturnsError() throws Exception {
        UUID targetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/social/follow/{targetId}", targetId))
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus())
                                .isNotEqualTo(204));

        verifyNoInteractions(socialService);
    }
}