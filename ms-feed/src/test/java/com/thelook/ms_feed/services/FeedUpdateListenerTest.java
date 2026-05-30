package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
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

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedUpdateListenerTest {

    @Mock OutfitElasticRepository elasticRepository;
    @Mock Channel channel;
    @InjectMocks FeedUpdateListener feedUpdateListener;

    private static final long TAG = 1L;

    @Test
    void handleFeedImageUpdate_tipoMain_atualizaImage1UrlEStatusEAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY),
                channel, TAG);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage1Url()).isEqualTo("processed.webp");
        assertThat(captor.getValue().getImageStatus()).isEqualTo("READY");
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void handleFeedImageUpdate_tipoSecondary_atualizaImage2UrlEAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed2.webp", "SECONDARY", ImageProcessStatus.READY),
                channel, TAG);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage2Url()).isEqualTo("processed2.webp");
        assertThat(captor.getValue().getImage1Url()).isNull();
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void handleFeedImageUpdate_tipoDesconhecido_naoAlteraCamposDeImagemEAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        OutfitDocument doc = docWith(outfitId);
        doc.setImage1Url("existente.webp");
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.of(doc));

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "novo.webp", "UNKNOWN", ImageProcessStatus.READY),
                channel, TAG);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(elasticRepository).save(captor.capture());
        assertThat(captor.getValue().getImage1Url()).isEqualTo("existente.webp");
        assertThat(captor.getValue().getImageStatus()).isNull();
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void handleFeedImageUpdate_outfitNaoEncontrado_naoSalvaMasAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        when(elasticRepository.findById(outfitId.toString())).thenReturn(Optional.empty());

        feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY),
                channel, TAG);

        verify(elasticRepository, never()).save(any());
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void handleFeedImageUpdate_excecaoNoRepositorio_nackSemPropagarExcecao() throws IOException {
        UUID outfitId = UUID.randomUUID();
        when(elasticRepository.findById(outfitId.toString()))
                .thenThrow(new RuntimeException("Elasticsearch fora"));

        assertThatCode(() -> feedUpdateListener.handleFeedImageUpdate(
                new ImageProcessedDTO(outfitId, "original.jpg", "processed.webp", "MAIN", ImageProcessStatus.READY),
                channel, TAG))
                .doesNotThrowAnyException();

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private OutfitDocument docWith(UUID outfitId) {
        OutfitDocument doc = new OutfitDocument();
        doc.setId(outfitId.toString());
        return doc;
    }
}