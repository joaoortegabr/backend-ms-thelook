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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of());

        outboxRelay.publishMessages();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void publishMessages_comMensagem_enviaoPayloadParaRabbit() {
        OutboxMessage msg = mensagem("{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        verify(rabbitTemplate).convertAndSend("ex.thelook.outfit", "outfit.created", "{\"outfitId\":\"abc\"}");
    }

    @Test
    void publishMessages_comMensagem_marcaComoProcessada() {
        OutboxMessage msg = mensagem("{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(msg));

        outboxRelay.publishMessages();

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().isProcessed()).isTrue();
    }

    @Test
    void publishMessages_rabbitFalha_naoMarcaComoProcessada() {
        OutboxMessage msg = mensagem("{\"outfitId\":\"abc\"}");
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(msg));
        doThrow(new RuntimeException("RabbitMQ fora")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        outboxRelay.publishMessages();

        verify(outboxRepository, never()).save(any());
        assertThat(msg.isProcessed()).isFalse();
    }

    @Test
    void publishMessages_multiplas_processaTodasIndependentemente() {
        OutboxMessage msg1 = mensagem("{\"id\":\"1\"}");
        OutboxMessage msg2 = mensagem("{\"id\":\"2\"}");
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(msg1, msg2));

        outboxRelay.publishMessages();

        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any());
        verify(outboxRepository, times(2)).save(any());
    }

    @Test
    void publishMessages_segundaFalha_primeiraAindaProcessada() {
        OutboxMessage msg1 = mensagem("{\"id\":\"1\"}");
        OutboxMessage msg2 = mensagem("{\"id\":\"2\"}");
        when(outboxRepository.findByProcessedFalse()).thenReturn(List.of(msg1, msg2));
        doNothing()
                .doThrow(new RuntimeException("falha na segunda"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        outboxRelay.publishMessages();

        verify(outboxRepository, times(1)).save(any());
        assertThat(msg2.isProcessed()).isFalse();
    }

    private OutboxMessage mensagem(String payload) {
        OutboxMessage msg = new OutboxMessage();
        msg.setAggregateId(UUID.randomUUID().toString());
        msg.setType("OUTFIT_CREATED");
        msg.setPayload(payload);
        return msg;
    }
}