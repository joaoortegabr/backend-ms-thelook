package com.thelook.ms_creation.services;

import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.repositories.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxRepository outboxRepository;
    @Mock RabbitTemplate rabbitTemplate;

    OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, rabbitTemplate);
    }

    // ── publishMessages ────────────────────────────────────────────────────────

    @Test
    void publishMessages_semMensagensPendentes_naoInterageComRabbit() {
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of());

        outboxRelay.publishMessages();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(outboxRepository, never()).markAsProcessed(any());
    }

    @Test
    void publishMessages_comOutfitCreated_enviaPorFeedSyncEMarcaProcessada() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "OUTFIT_CREATED", "{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        verify(rabbitTemplate).convertAndSend("ex.thelook.outfit", "feed.sync", "{\"outfitId\":\"abc\"}");
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    @Test
    void publishMessages_comOutfitUpdated_enviaPorFeedSync() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "OUTFIT_UPDATED", "{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        verify(rabbitTemplate).convertAndSend("ex.thelook.outfit", "feed.sync", "{\"outfitId\":\"abc\"}");
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    @Test
    void publishMessages_comOutfitDeleted_enviaPorOutfitDeletedKey() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "OUTFIT_DELETED", "{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        verify(rabbitTemplate).convertAndSend("ex.thelook.outfit", "outfit.deleted", "{\"outfitId\":\"abc\"}");
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    @Test
    void publishMessages_rabbitFalha_naoMarcaComoProcessada() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "OUTFIT_CREATED", "{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg));
        doThrow(new RuntimeException("RabbitMQ fora")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        outboxRelay.publishMessages();

        verify(outboxRepository, never()).markAsProcessed(any());
    }

    @Test
    void publishMessages_multiplas_processaTodasEMarcaNoBatch() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OutboxMessage msg1 = mensagem(id1, "OUTFIT_CREATED", "{\"id\":\"1\"}");
        OutboxMessage msg2 = mensagem(id2, "OUTFIT_CREATED", "{\"id\":\"2\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg1, msg2));

        outboxRelay.publishMessages();

        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).markAsProcessed(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    void publishMessages_segundaFalha_soMarcaPrimeiraComoProcessada() {
        UUID id1 = UUID.randomUUID();
        OutboxMessage msg1 = mensagem(id1, "OUTFIT_CREATED", "{\"id\":\"1\"}");
        OutboxMessage msg2 = mensagem(UUID.randomUUID(), "OUTFIT_CREATED", "{\"id\":\"2\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg1, msg2));
        doNothing()
                .doThrow(new RuntimeException("falha na segunda"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        outboxRelay.publishMessages();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).markAsProcessed(captor.capture());
        assertThat(captor.getValue()).containsExactly(id1);
    }

    @Test
    void publishMessages_tipoDesconhecido_marcaComoProcessadoSemEnvio() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "UNKNOWN_TYPE", "{\"id\":\"1\"}");
        when(outboxRepository.findByProcessedFalse(any(Pageable.class))).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    private OutboxMessage mensagem(UUID id, String type, String payload) {
        OutboxMessage msg = new OutboxMessage();
        msg.setId(id);
        msg.setAggregateId(UUID.randomUUID().toString());
        msg.setType(type);
        msg.setPayload(payload);
        return msg;
    }
}
