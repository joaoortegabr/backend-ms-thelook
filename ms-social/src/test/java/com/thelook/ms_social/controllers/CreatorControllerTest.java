package com.thelook.ms_social.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.config.SecurityConfig;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorResponse;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.models.mappers.CreatorMapper;
import com.thelook.ms_social.services.CreatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CreatorController.class)
@Import(SecurityConfig.class)
class CreatorControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean CreatorService creatorService;
    @MockitoBean CreatorMapper mapper;

    private CreatorResponse response(UUID id) {
        return new CreatorResponse(id, "Alice", null, "Bio", "alice_ig",
                LocalDate.of(1995, 1, 1), "São Paulo", "SP", 0L);
    }

    private CreatorRequest validRequest() {
        return new CreatorRequest("Alice", null, "Bio", "alice_ig",
                LocalDate.of(1995, 1, 1), "São Paulo", "SP");
    }

    // =========================================================
    // GET /{creatorId}
    // =========================================================

    @Test
    void findById_creatorExists_returns200WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        Creator creator = new Creator();
        creator.setId(id);
        CreatorResponse response = response(id);

        when(creatorService.findById(id)).thenReturn(creator);
        when(mapper.toCreatorResponse(creator)).thenReturn(response);

        mockMvc.perform(get("/api/v1/creators/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.instagram").value("alice_ig"));
    }

    @Test
    void findById_creatorNotFound_returns4xx() throws Exception {
        UUID id = UUID.randomUUID();
        when(creatorService.findById(id)).thenThrow(new ResourceNotFoundException(id));

        mockMvc.perform(get("/api/v1/creators/{id}", id))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // POST /
    // =========================================================

    @Test
    void create_validRequest_returns201WithLocationHeader() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Creator creator = new Creator();
        creator.setId(creatorId);
        CreatorResponse response = response(creatorId);

        when(creatorService.create(eq(userId), any())).thenReturn(creator);
        when(mapper.toCreatorResponse(creator)).thenReturn(response);

        mockMvc.perform(post("/api/v1/creators")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(creatorId.toString()));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        CreatorRequest req = new CreatorRequest("", null, "Bio", "alice_ig", null, null, "SP");

        mockMvc.perform(post("/api/v1/creators")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(creatorService);
    }

    @Test
    void create_blankInstagram_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        CreatorRequest req = new CreatorRequest("Alice", null, "Bio", "", null, null, "SP");

        mockMvc.perform(post("/api/v1/creators")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(creatorService);
    }

    @Test
    void create_ufWithWrongSize_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        CreatorRequest req = new CreatorRequest("Alice", null, "Bio", "alice_ig", null, null, "SPX");

        mockMvc.perform(post("/api/v1/creators")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(creatorService);
    }

    @Test
    void create_missingUserIdHeader_returns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/creators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // PUT /
    // =========================================================

    @Test
    void update_validRequest_returns200WithUpdatedCreator() throws Exception {
        UUID creatorId = UUID.randomUUID();
        Creator creator = new Creator();
        creator.setId(creatorId);
        CreatorResponse response = response(creatorId);
        CreatorUpdateRequest req = new CreatorUpdateRequest("new-avatar", "new bio", "new_ig", "SP", "SP");

        when(creatorService.update(eq(creatorId), any())).thenReturn(creator);
        when(mapper.toCreatorResponse(creator)).thenReturn(response);

        mockMvc.perform(put("/api/v1/creators")
                        .header("X-Creator-Id", creatorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(creatorId.toString()));
    }

    @Test
    void update_missingCreatorIdHeader_returns4xx() throws Exception {
        mockMvc.perform(put("/api/v1/creators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatorUpdateRequest(null, null, null, null, null))))
                .andExpect(status().is4xxClientError());
    }

    // =========================================================
    // DELETE /
    // =========================================================

    @Test
    void delete_validCreatorId_returns200WithMessage() throws Exception {
        UUID creatorId = UUID.randomUUID();
        when(creatorService.delete(creatorId)).thenReturn("Registro removido com sucesso.");

        mockMvc.perform(delete("/api/v1/creators")
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registro removido com sucesso."));
    }

    @Test
    void delete_missingCreatorIdHeader_returns4xx() throws Exception {
        mockMvc.perform(delete("/api/v1/creators"))
                .andExpect(status().is4xxClientError());
    }
}