package com.thelook.ms_social.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.ms_social.entities.OutboxMessage;
import com.thelook.ms_social.repositories.OutboxRepository;
import com.thelook.ms_social.services.events.OutboxSavedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorEventPublisherTest {

    @Mock OutboxRepository outboxRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    CreatorEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CreatorEventPublisher(outboxRepository, new ObjectMapper(), eventPublisher);
    }

    // =========================================================
    // publishDeactivated
    // =========================================================

    @Test
    void publishDeactivated_gravaNoOutboxComTipoCorreto() {
        UUID creatorId = UUID.randomUUID();

        publisher.publishDeactivated(creatorId);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());

        OutboxMessage saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("CREATOR_DEACTIVATED");
        assertThat(saved.getAggregateId()).isEqualTo(creatorId.toString());
        assertThat(saved.getPayload()).contains("DEACTIVATED");
        assertThat(saved.getPayload()).contains(creatorId.toString());
    }

    @Test
    void publishDeactivated_publicaOutboxSavedEvent() {
        publisher.publishDeactivated(UUID.randomUUID());

        verify(eventPublisher).publishEvent(any(OutboxSavedEvent.class));
    }

    // =========================================================
    // publishReactivated
    // =========================================================

    @Test
    void publishReactivated_gravaNoOutboxComTipoCorreto() {
        UUID creatorId = UUID.randomUUID();

        publisher.publishReactivated(creatorId);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());

        OutboxMessage saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("CREATOR_REACTIVATED");
        assertThat(saved.getAggregateId()).isEqualTo(creatorId.toString());
        assertThat(saved.getPayload()).contains("REACTIVATED");
    }

    // =========================================================
    // publishPurged
    // =========================================================

    @Test
    void publishPurged_gravaNoOutboxComCreatorIdEUserId() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        publisher.publishPurged(creatorId, userId);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());

        OutboxMessage saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("CREATOR_PURGED");
        assertThat(saved.getAggregateId()).isEqualTo(creatorId.toString());
        assertThat(saved.getPayload()).contains("PURGED");
        assertThat(saved.getPayload()).contains(userId.toString());
    }

    // =========================================================
    // Garantia: não interage mais com RabbitMQ diretamente
    // =========================================================

    @Test
    void publish_naoInterageDiretamenteComRabbitMQ() {
        publisher.publishDeactivated(UUID.randomUUID());
        publisher.publishReactivated(UUID.randomUUID());
        publisher.publishPurged(UUID.randomUUID(), UUID.randomUUID());

        // Verificação estrutural: o publisher não injeta mais RabbitTemplate
        // Apenas outboxRepository e eventPublisher são chamados
        verify(outboxRepository, times(3)).save(any(OutboxMessage.class));
        verify(eventPublisher, times(3)).publishEvent(any(OutboxSavedEvent.class));
    }

    @Test
    void publishDeactivated_repositorioFalha_propagaExcecaoENaoPublicaEvento() {
        doThrow(new RuntimeException("DB fora")).when(outboxRepository).save(any(OutboxMessage.class));

        assertThatThrownBy(() -> publisher.publishDeactivated(UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB fora");

        verify(eventPublisher, never()).publishEvent(any());
    }
}