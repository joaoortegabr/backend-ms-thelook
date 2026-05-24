package com.thelook.ms_feed.services;

import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedUpdateListenerTest {

    @Mock OutfitElasticRepository elasticRepository;
    @InjectMocks FeedUpdateListener feedUpdateListener;

    @Test
    void handleFeedImageUpdate_tipoMain_atualizaImage1UrlEStatus() {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY));

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage1Url()).isEqualTo("processed.webp");
        assertThat(captor.getValue().getImageStatus()).isEqualTo("READY");
    }

    @Test
    void handleFeedImageUpdate_tipoSecondary_atualizaImage2Url() {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed2.webp", "SECONDARY", ImageProcessStatus.READY));

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage2Url()).isEqualTo("processed2.webp");
        assertThat(captor.getValue().getImage1Url()).isNull();
    }

    @Test
    void handleFeedImageUpdate_tipoDesconhecido_naoAlteraCamposDeImagem() {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        doc.setImage1Url("existente.webp");
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "novo.webp", "UNKNOWN", ImageProcessStatus.READY));

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage1Url()).isEqualTo("existente.webp");
        assertThat(captor.getValue().getImageStatus()).isNull();
    }

    @Test
    void handleFeedImageUpdate_outfitNaoEncontrado_naoSalva() {
        UUID outfitId = UUID.randomUUID();
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.empty());

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY));

        verify(elasticRepository, never()).save(any());
    }

    @Test
    void handleFeedImageUpdate_excecaoNoRepositorio_propagaErro() {
        UUID outfitId = UUID.randomUUID();
        when(elasticRepository.findById(outfitId.toString()))
                .thenThrow(new RuntimeException("Elasticsearch fora"));

        assertThatThrownBy(() -> feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch fora");
    }

    private OutfitDocument docWith(UUID outfitId) {
        OutfitDocument doc = new OutfitDocument();
        doc.setId(outfitId.toString());
        return doc;
    }
}