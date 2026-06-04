package com.thelook.ms_social.services;

import com.thelook.ms_social.entities.OutboxMessage;
import com.thelook.ms_social.repositories.OutboxRepository;
import com.thelook.ms_social.services.events.OutboxSavedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock OutboxRepository outboxRepository;
    @Mock RabbitTemplate rabbitTemplate;

    OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, rabbitTemplate);
    }

    @Test
    void publishMessages_semMensagensPendentes_naoInterageComRabbit() {
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of());

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate, never()).invoke(any());
        verify(outboxRepository, never()).markAsProcessed(any());
    }

    @Test
    void publishMessages_comCreatorDeactivated_invocaRabbitEMarcaProcessada() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "CREATOR_DEACTIVATED", "{\"type\":\"DEACTIVATED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate).invoke(any());
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
        verify(outboxRepository, never()).incrementRetryCount(any());
    }

    @Test
    void publishMessages_comCreatorReactivated_invocaRabbitEMarcaProcessada() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "CREATOR_REACTIVATED", "{\"type\":\"REACTIVATED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate).invoke(any());
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    @Test
    void publishMessages_comCreatorPurged_invocaRabbitEMarcaProcessada() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "CREATOR_PURGED", "{\"type\":\"PURGED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate).invoke(any());
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
    }

    @Test
    void publishMessages_rabbitFalha_naoMarcaComoProcessadaEIncrementaRetry() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "CREATOR_DEACTIVATED", "{\"type\":\"DEACTIVATED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg));
        when(rabbitTemplate.invoke(any())).thenThrow(new RuntimeException("RabbitMQ fora"));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(outboxRepository, never()).markAsProcessed(any());
        verify(outboxRepository).incrementRetryCount(msg.getId());
    }

    @Test
    void publishMessages_multiplas_invocaRabbitParaCadaEMarcaNoBatch() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OutboxMessage msg1 = mensagem(id1, "CREATOR_DEACTIVATED", "{\"type\":\"DEACTIVATED\"}");
        OutboxMessage msg2 = mensagem(id2, "CREATOR_PURGED", "{\"type\":\"PURGED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg1, msg2));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate, times(2)).invoke(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).markAsProcessed(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(id1, id2);
    }

    @Test
    void publishMessages_segundaFalha_soMarcaPrimeiraEIncrementaRetryDaSegunda() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        OutboxMessage msg1 = mensagem(id1, "CREATOR_DEACTIVATED", "{\"type\":\"DEACTIVATED\"}");
        OutboxMessage msg2 = mensagem(id2, "CREATOR_REACTIVATED", "{\"type\":\"REACTIVATED\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg1, msg2));
        when(rabbitTemplate.invoke(any()))
                .thenReturn(null)
                .thenThrow(new RuntimeException("falha na segunda"));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxRepository).markAsProcessed(captor.capture());
        assertThat(captor.getValue()).containsExactly(id1);
        verify(outboxRepository).incrementRetryCount(id2);
        verify(outboxRepository, never()).incrementRetryCount(id1);
    }

    @Test
    void publishMessages_tipoDesconhecido_marcaComoProcessadoSemInvocar() {
        OutboxMessage msg = mensagem(UUID.randomUUID(), "UNKNOWN_TYPE", "{\"type\":\"?\"}");
        when(outboxRepository.findByProcessedFalseAndRetryCountLessThan(anyInt(), any(Pageable.class)))
                .thenReturn(List.of(msg));

        outboxRelay.onOutboxSaved(new OutboxSavedEvent());
        outboxRelay.publishMessages();

        verify(rabbitTemplate, never()).invoke(any());
        verify(outboxRepository).markAsProcessed(List.of(msg.getId()));
        verify(outboxRepository, never()).incrementRetryCount(any());
    }

    @Test
    void publishMessages_semDirtyFlag_naoConsultaRepositorio() {
        outboxRelay.publishMessages();

        verify(outboxRepository, never()).findByProcessedFalseAndRetryCountLessThan(anyInt(), any());
    }

    @Test
    void purgeProcessedMessages_delegaAoRepositorioComCutoffCorreto() {
        outboxRelay.purgeProcessedMessages();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxRepository).deleteProcessedBefore(captor.capture());
        assertThat(captor.getValue()).isBefore(LocalDateTime.now().minusDays(6));
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