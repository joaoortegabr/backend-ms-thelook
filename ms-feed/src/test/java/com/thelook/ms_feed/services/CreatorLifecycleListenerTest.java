package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorLifecycleListenerTest {

    @Mock OutfitElasticRepository repository;
    @Mock Channel channel;

    @InjectMocks CreatorLifecycleListener listener;

    // =========================================================
    // DEACTIVATED
    // =========================================================

    @Test
    @SuppressWarnings("unchecked")
    void handle_deactivatedEvent_setsActiveFalseOnAllOutfitsAndAcks() throws IOException {
        UUID creatorId = UUID.randomUUID();
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");
        doc.setIsActive(true);
        when(repository.findByCreatorId(creatorId.toString())).thenReturn(List.of(doc));

        listener.handle(new CreatorLifecycleEventDTO("DEACTIVATED", creatorId, null), channel, 1L);

        ArgumentCaptor<List<OutfitDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getIsActive()).isFalse();
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @SuppressWarnings("unchecked")
    void handle_reactivatedEvent_setsActiveTrueOnAllOutfitsAndAcks() throws IOException {
        UUID creatorId = UUID.randomUUID();
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");
        doc.setIsActive(false);
        when(repository.findByCreatorId(creatorId.toString())).thenReturn(List.of(doc));

        listener.handle(new CreatorLifecycleEventDTO("REACTIVATED", creatorId, null), channel, 2L);

        ArgumentCaptor<List<OutfitDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getIsActive()).isTrue();
        verify(channel).basicAck(2L, false);
    }

    @Test
    void handle_deactivatedEvent_noOutfits_savesEmptyListAndAcks() throws IOException {
        UUID creatorId = UUID.randomUUID();
        when(repository.findByCreatorId(creatorId.toString())).thenReturn(List.of());

        listener.handle(new CreatorLifecycleEventDTO("DEACTIVATED", creatorId, null), channel, 3L);

        verify(repository).saveAll(List.of());
        verify(channel).basicAck(3L, false);
    }

    // =========================================================
    // PURGED
    // =========================================================

    @Test
    void handle_purgedEvent_deletesAllOutfitsByCreatorIdAndAcks() throws IOException {
        UUID creatorId = UUID.randomUUID();

        listener.handle(new CreatorLifecycleEventDTO("PURGED", creatorId, null), channel, 4L);

        verify(repository).deleteByCreatorId(creatorId.toString());
        verify(channel).basicAck(4L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    // =========================================================
    // Unknown type
    // =========================================================

    @Test
    void handle_unknownEventType_doesNotModifyDocumentsAndAcks() throws IOException {
        UUID creatorId = UUID.randomUUID();

        listener.handle(new CreatorLifecycleEventDTO("UNKNOWN", creatorId, null), channel, 5L);

        verify(repository, never()).saveAll(any());
        verify(repository, never()).deleteByCreatorId(any());
        verify(channel).basicAck(5L, false);
    }

    // =========================================================
    // Error handling — nack on exception
    // =========================================================

    @Test
    void handle_repositoryThrows_nacksWithoutRequeueAndDoesNotAck() throws IOException {
        UUID creatorId = UUID.randomUUID();
        doThrow(new RuntimeException("ES failure")).when(repository).deleteByCreatorId(creatorId.toString());

        listener.handle(new CreatorLifecycleEventDTO("PURGED", creatorId, null), channel, 6L);

        verify(channel).basicNack(6L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}