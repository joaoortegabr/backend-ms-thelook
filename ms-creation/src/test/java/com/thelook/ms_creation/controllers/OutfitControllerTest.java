package com.thelook.ms_creation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.dtos.OutfitResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

        mockMvc.perform(get("/api/v1/creation/{id}", outfitId))
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

        when(outfitService.createOutfit(eq(creatorId), any(), any(), any(), any())).thenReturn(saved);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1Part = new MockMultipartFile(
                "image1", "image1.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/v1/creation/outfit")
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

        Outfit saved = new Outfit();
        saved.setId(UUID.randomUUID());
        when(outfitService.createOutfit(any(), any(), any(), any(), any())).thenReturn(saved);

        MockMultipartFile requestPart = new MockMultipartFile(
                "request", "request", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request));
        MockMultipartFile image1 = new MockMultipartFile("image1", "i1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile image2 = new MockMultipartFile("image2", "i2.jpg", "image/jpeg", new byte[]{2});

        mockMvc.perform(multipart("/api/v1/creation/outfit")
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

        mockMvc.perform(multipart("/api/v1/creation/outfit")
                        .file(requestPart)
                        .file(image1))
                .andExpect(status().isBadRequest());
    }
}