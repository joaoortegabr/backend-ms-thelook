package com.thelook.ms_feed.services;

import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedListenerTest {

    @Mock OutfitIndexService outfitIndexService;
    @InjectMocks FeedListener feedListener;

    @Test
    void handleOutfitCreated_sucesso_delegaParaIndexService() {
        OutfitSyncDTO dto = outfitSyncDTO();

        feedListener.handleOutfitCreated(dto);

        verify(outfitIndexService).index(dto);
    }

    @Test
    void handleOutfitCreated_excecaoNoIndex_propagaErro() {
        OutfitSyncDTO dto = outfitSyncDTO();
        doThrow(new RuntimeException("Elasticsearch indisponivel")).when(outfitIndexService).index(dto);

        assertThatThrownBy(() -> feedListener.handleOutfitCreated(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Elasticsearch indisponivel");
    }

    private OutfitSyncDTO outfitSyncDTO() {
        return new OutfitSyncDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Look", null, null,
                "CASUAL", null, null, ImageProcessStatus.PENDING
        );
    }
}