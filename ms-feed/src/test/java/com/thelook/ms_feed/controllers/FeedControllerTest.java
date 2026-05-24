package com.thelook.ms_feed.controllers;

import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.services.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FeedControllerTest {

    @Mock FeedService feedService;
    @InjectMocks FeedController feedController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feedController).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFeed_retornaOutfitsETotal() throws Exception {
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");

        SearchHit<OutfitDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hit.getSortValues()).thenReturn(List.of("2024-01-01T10:00:00", "outfit-1"));

        SearchHits<OutfitDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(hits.getTotalHits()).thenReturn(1L);
        when(feedService.searchFeed(any(), any(), any(), any(), anyInt(), any())).thenReturn(hits);

        mockMvc.perform(get("/api/v1/outfits/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.outfits[0].id").value("outfit-1"))
                .andExpect(jsonPath("$.nextSortValues").isArray());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFeed_semResultados_retornaListaVaziaENextSortValuesVazio() throws Exception {
        SearchHits<OutfitDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(feedService.searchFeed(any(), any(), any(), any(), anyInt(), any())).thenReturn(hits);

        mockMvc.perform(get("/api/v1/outfits/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.outfits").isEmpty())
                .andExpect(jsonPath("$.nextSortValues").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getFeed_comFiltros_passaParametrosCorretosParaServico() throws Exception {
        SearchHits<OutfitDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(feedService.searchFeed(eq("CASUAL"), any(), eq("SHOES"), eq("verao"), eq(10), any()))
                .thenReturn(hits);

        mockMvc.perform(get("/api/v1/outfits/feed")
                        .param("style", "CASUAL")
                        .param("itemType", "SHOES")
                        .param("title", "verao")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_quandoEncontrado_retorna200ComDocumento() throws Exception {
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");
        when(feedService.findById("outfit-1")).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/v1/outfits/outfit-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("outfit-1"));
    }

    @Test
    void getById_quandoNaoEncontrado_retorna404() throws Exception {
        when(feedService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/outfits/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByCreator_retornaListaDeOutfits() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");
        when(feedService.findByCreatorId(creatorId)).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/outfits/creator/" + creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("outfit-1"));
    }

    @Test
    void getByCreator_semOutfits_retornaListaVazia() throws Exception {
        UUID creatorId = UUID.randomUUID();
        when(feedService.findByCreatorId(creatorId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/outfits/creator/" + creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}