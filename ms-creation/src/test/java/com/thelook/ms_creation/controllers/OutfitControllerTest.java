package com.thelook.ms_creation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.dtos.OutfitResponse;
import com.thelook.ms_creation.models.enums.ItemType;
import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.services.ItemService;
import com.thelook.ms_creation.services.OutfitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OutfitControllerTest {

    @Mock OutfitService outfitService;
    @Mock ItemService itemService;
    @Mock OutfitMapper outfitMapper;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        OutfitController controller = new OutfitController(outfitService, itemService, outfitMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_quandoEncontrado_retorna200ComOutfitResponse() throws Exception {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = new Outfit();
        outfit.setId(outfitId);

        OutfitResponse response = new OutfitResponse(outfitId, UUID.randomUUID(),
                "Look", null, null, OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null, List.of());

        when(outfitService.findById(outfitId)).thenReturn(outfit);
        when(outfitMapper.toOutfitResponse(outfit)).thenReturn(response);

        mockMvc.perform(get("/api/v1/creation/outfits/{id}", outfitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outfitId").value(outfitId.toString()))
                .andExpect(jsonPath("$.title").value("Look"));
    }

    // ── createOutfit ──────────────────────────────────────────────────────────

    @Test
    void createOutfit_comDadosValidos_retorna201() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();

        OutfitRequest request = new OutfitRequest("Look casual", OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null);

        Outfit saved = new Outfit();
        saved.setId(outfitId);
        saved.setCreatorId(creatorId);

        OutfitResponse response = new OutfitResponse(outfitId, creatorId, "Look casual",
                null, null, OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null, List.of());
        when(outfitService.createOutfit(eq(creatorId), any(), any(), any(), any())).thenReturn(saved);
        when(outfitMapper.toOutfitResponse(saved)).thenReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1Part = new MockMultipartFile(
                "image1", "image1.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/creation/outfits")
                        .file(requestPart)
                        .file(image1Part)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isCreated());

        verify(outfitService).createOutfit(eq(creatorId), any(), any(), any(), any());
    }

    @Test
    void createOutfit_comSegundaImagem_passaParaServico() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.FORMAL, Set.of(OutfitColor.WHITE), null);

        UUID outfitId = UUID.randomUUID();
        Outfit saved = new Outfit();
        saved.setId(outfitId);
        OutfitResponse response = new OutfitResponse(outfitId, creatorId, "Look",
                null, null, OutfitStyle.FORMAL, Set.of(OutfitColor.WHITE), null, List.of());
        when(outfitService.createOutfit(any(), any(), any(), any(), any())).thenReturn(saved);
        when(outfitMapper.toOutfitResponse(saved)).thenReturn(response);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1 = new MockMultipartFile("image1", "i1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile image2 = new MockMultipartFile("image2", "i2.jpg", "image/jpeg", new byte[]{2});

        mockMvc.perform(multipart("/api/v1/creation/outfits")
                        .file(requestPart)
                        .file(image1)
                        .file(image2)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isCreated());

        verify(outfitService).createOutfit(any(), any(), any(), any(), any());
    }

    @Test
    void createOutfit_semCreatorIdHeader_retorna400() throws Exception {
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1 = new MockMultipartFile("image1", "i1.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/creation/outfits")
                        .file(requestPart)
                        .file(image1))
                .andExpect(status().isBadRequest());
    }

    // ── deleteOutfit ──────────────────────────────────────────────────────────

    @Test
    void deleteOutfit_comDadosValidos_retorna204() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{id}", outfitId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isNoContent());

        verify(outfitService).delete(outfitId, creatorId);
    }

    @Test
    void deleteOutfit_semCreatorIdHeader_retorna400() throws Exception {
        UUID outfitId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{id}", outfitId))
                .andExpect(status().isBadRequest());
    }

    // ── hardDeleteOutfit ──────────────────────────────────────────────────────

    @Test
    void hardDeleteOutfit_comDadosValidos_retorna204() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{id}/hard", outfitId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isNoContent());

        verify(outfitService).hardDelete(outfitId, creatorId);
    }

    @Test
    void hardDeleteOutfit_semCreatorIdHeader_retorna400() throws Exception {
        UUID outfitId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{id}/hard", outfitId))
                .andExpect(status().isBadRequest());
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    void addItem_comDadosValidos_retorna201() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        ItemRequest itemRequest = new ItemRequest(
                ItemType.SHIRT, "White Shirt", null,
                "https://example.com/product", ImageProcessStatus.PENDING);

        ItemResponse itemResponse = new ItemResponse(itemId, ItemType.SHIRT, "White Shirt",
                null, "https://example.com/product", ImageProcessStatus.PENDING);

        when(itemService.addItem(eq(outfitId), eq(creatorId), any(), any())).thenReturn(itemResponse);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(itemRequest));

        mockMvc.perform(multipart("/api/v1/creation/outfits/{id}/items", outfitId)
                        .file(requestPart)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isCreated());

        verify(itemService).addItem(eq(outfitId), eq(creatorId), any(), any());
    }

    @Test
    void addItem_semCreatorIdHeader_retorna400() throws Exception {
        UUID outfitId = UUID.randomUUID();
        ItemRequest itemRequest = new ItemRequest(
                ItemType.SHIRT, "White Shirt", null,
                "https://example.com/product", null);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(itemRequest));

        mockMvc.perform(multipart("/api/v1/creation/outfits/{id}/items", outfitId)
                        .file(requestPart))
                .andExpect(status().isBadRequest());
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test
    void deleteItem_comDadosValidos_retorna204() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{outfitId}/items/{itemId}", outfitId, itemId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isNoContent());

        verify(itemService).deleteItem(outfitId, itemId, creatorId);
    }

    @Test
    void deleteItem_semCreatorIdHeader_retorna400() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{outfitId}/items/{itemId}", outfitId, itemId))
                .andExpect(status().isBadRequest());
    }

    // ── hardDeleteItem ────────────────────────────────────────────────────────

    @Test
    void hardDeleteItem_comDadosValidos_retorna204() throws Exception {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{outfitId}/items/{itemId}/hard", outfitId, itemId)
                        .header("X-Creator-Id", creatorId.toString()))
                .andExpect(status().isNoContent());

        verify(itemService).hardDeleteItem(outfitId, itemId, creatorId);
    }

    @Test
    void hardDeleteItem_semCreatorIdHeader_retorna400() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/creation/outfits/{outfitId}/items/{itemId}/hard", outfitId, itemId))
                .andExpect(status().isBadRequest());
    }
}