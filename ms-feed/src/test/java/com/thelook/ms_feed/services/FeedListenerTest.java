package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.OutfitDeletedDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedListenerTest {

    @Mock OutfitIndexService outfitIndexService;
    @Mock Channel channel;
    @InjectMocks FeedListener feedListener;

    private static final long TAG = 1L;

    @Test
    void handleOutfitCreated_sucesso_delegaParaIndexServiceEAckMensagem() throws IOException {
        OutfitSyncDTO dto = outfitSyncDTO();

        feedListener.handleOutfitCreated(dto, channel, TAG);

        verify(outfitIndexService).index(dto);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void handleOutfitCreated_excecaoNoIndex_nackMensagemSemPropagarExcecao() throws IOException {
        OutfitSyncDTO dto = outfitSyncDTO();
        doThrow(new RuntimeException("Elasticsearch indisponivel")).when(outfitIndexService).index(dto);

        assertThatCode(() -> feedListener.handleOutfitCreated(dto, channel, TAG))
                .doesNotThrowAnyException();

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void handleOutfitDeleted_sucesso_delegaParaIndexServiceEAckMensagem() throws IOException {
        OutfitDeletedDTO dto = new OutfitDeletedDTO(UUID.randomUUID(), UUID.randomUUID());

        feedListener.handleOutfitDeleted(dto, channel, TAG);

        verify(outfitIndexService).removeById(dto.outfitId());
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void handleOutfitDeleted_excecaoNoRemove_nackMensagemSemPropagarExcecao() throws IOException {
        OutfitDeletedDTO dto = new OutfitDeletedDTO(UUID.randomUUID(), UUID.randomUUID());
        doThrow(new RuntimeException("Elasticsearch indisponivel"))
                .when(outfitIndexService).removeById(dto.outfitId());

        assertThatCode(() -> feedListener.handleOutfitDeleted(dto, channel, TAG))
                .doesNotThrowAnyException();

        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private OutfitSyncDTO outfitSyncDTO() {
        return new OutfitSyncDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Look", null, null,
                "CASUAL", null, null, ImageProcessStatus.PENDING
        );
    }
}