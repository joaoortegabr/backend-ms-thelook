package com.thelook.ms_creation.services;

import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.enums.ItemType;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageStatusListenerTest {

    @Mock OutfitRepository repository;

    ImageStatusListener listener;

    @BeforeEach
    void setUp() {
        listener = new ImageStatusListener(repository);
    }

    @Test
    void handleImageProcessed_tipoOUTFIT_atualizaImage1UrlEStatusReady() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitVazio(outfitId);
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        listener.handleImageProcessed(dto(outfitId, "original.jpg", "processed.webp", "OUTFIT"));

        ArgumentCaptor<Outfit> captor = ArgumentCaptor.forClass(Outfit.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getImage1Url()).isEqualTo("processed.webp");
        assertThat(captor.getValue().getImageStatus()).isEqualTo(ImageProcessStatus.READY);
    }

    @Test
    void handleImageProcessed_tipoSECONDARY_atualizaImage2Url() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitVazio(outfitId);
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        listener.handleImageProcessed(dto(outfitId, "original.jpg", "processed2.webp", "SECONDARY"));

        ArgumentCaptor<Outfit> captor = ArgumentCaptor.forClass(Outfit.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getImage2Url()).isEqualTo("processed2.webp");
        assertThat(captor.getValue().getImage1Url()).isNull();
    }

    @Test
    void handleImageProcessed_tipoITEM_atualizaItemQueMatchaOriginalPath() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitVazio(outfitId);
        Item item = new Item();
        item.setItemType(ItemType.SHIRT);
        item.setItemImg("original_item.jpg");
        outfit.getItems().add(item);
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        listener.handleImageProcessed(dto(outfitId, "original_item.jpg", "processed_item.webp", "ITEM"));

        verify(repository).save(outfit);
        assertThat(item.getItemImg()).isEqualTo("processed_item.webp");
        assertThat(item.getImageStatus()).isEqualTo(ImageProcessStatus.READY);
    }

    @Test
    void handleImageProcessed_tipoITEM_semMatchOriginalPath_naoAlteraItem() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitVazio(outfitId);
        Item item = new Item();
        item.setItemImg("outro_arquivo.jpg");
        outfit.getItems().add(item);
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        listener.handleImageProcessed(dto(outfitId, "nao_existe.jpg", "processed.webp", "ITEM"));

        verify(repository).save(outfit);
        assertThat(item.getItemImg()).isEqualTo("outro_arquivo.jpg");
        assertThat(item.getImageStatus()).isNull();
    }

    @Test
    void handleImageProcessed_tipoDesconhecido_naoAlteraCamposDeImagem() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitVazio(outfitId);
        outfit.setImage1Url("existente.jpg");
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        listener.handleImageProcessed(dto(outfitId, "original.jpg", "novo.webp", "UNKNOWN"));

        verify(repository).save(outfit);
        assertThat(outfit.getImage1Url()).isEqualTo("existente.jpg");
        assertThat(outfit.getImageStatus()).isNull();
    }

    @Test
    void handleImageProcessed_outfitNaoEncontrado_naoSalva() {
        UUID outfitId = UUID.randomUUID();
        when(repository.findWithItemsById(outfitId)).thenReturn(Optional.empty());

        listener.handleImageProcessed(dto(outfitId, "original.jpg", "processed.webp", "OUTFIT"));

        verify(repository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Outfit outfitVazio(UUID id) {
        Outfit outfit = new Outfit();
        outfit.setId(id);
        return outfit;
    }

    private ImageProcessedDTO dto(UUID outfitId, String originalPath, String processedPath, String type) {
        return new ImageProcessedDTO(outfitId, originalPath, processedPath, type, ImageProcessStatus.READY);
    }
}