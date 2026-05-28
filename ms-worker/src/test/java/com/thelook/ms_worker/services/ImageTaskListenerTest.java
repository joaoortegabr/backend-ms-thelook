package com.thelook.ms_worker.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.ImageProcessedDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageTaskListenerTest {

    @Mock ImageWorkerService imageWorkerService;
    @Mock RabbitTemplate rabbitTemplate;
    @Mock Channel channel;

    @InjectMocks ImageTaskListener imageTaskListener;

    // ── handleHighPriority ─────────────────────────────────────────────────────

    @Test
    void handleHighPriority_sucesso_processaEnviaNotificacaoEAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        Map<String, Object> payload = buildPayload(outfitId, List.of("outfit/image.jpg"));
        when(imageWorkerService.processToWebp("outfit/image.jpg")).thenReturn("outfit/processed/image.webp");

        imageTaskListener.handleHighPriority(payload, channel, 1L);

        verify(imageWorkerService).processToWebp("outfit/image.jpg");
        ArgumentCaptor<ImageProcessedDTO> captor = ArgumentCaptor.forClass(ImageProcessedDTO.class);
        verify(rabbitTemplate).convertAndSend(eq("ex.thelook.outfit"), eq("image.status.updated"), captor.capture());
        assertThat(captor.getValue().outfitId()).isEqualTo(outfitId);
        assertThat(captor.getValue().type()).isEqualTo("OUTFIT");
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void handleHighPriority_erro_naoProcessaENack() throws IOException {
        Map<String, Object> payload = buildPayload(UUID.randomUUID(), List.of("outfit/image.jpg"));
        when(imageWorkerService.processToWebp(anyString())).thenThrow(new RuntimeException("Scrimage falhou"));

        imageTaskListener.handleHighPriority(payload, channel, 2L);

        verify(channel).basicNack(2L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any());
    }

    @Test
    void handleHighPriority_semImagens_ackSemProcessar() throws IOException {
        Map<String, Object> payload = buildPayload(UUID.randomUUID(), null);

        imageTaskListener.handleHighPriority(payload, channel, 3L);

        verify(imageWorkerService, never()).processToWebp(anyString());
        verify(channel).basicAck(3L, false);
    }

    // ── handleLowPriority ──────────────────────────────────────────────────────

    @Test
    void handleLowPriority_sucesso_processaComTypeItemEAck() throws IOException {
        UUID outfitId = UUID.randomUUID();
        Map<String, Object> payload = buildPayload(outfitId, List.of("item/image.jpg"));
        when(imageWorkerService.processToWebp("item/image.jpg")).thenReturn("item/processed/image.webp");

        imageTaskListener.handleLowPriority(payload, channel, 4L);

        verify(imageWorkerService).processToWebp("item/image.jpg");
        ArgumentCaptor<ImageProcessedDTO> captor = ArgumentCaptor.forClass(ImageProcessedDTO.class);
        verify(rabbitTemplate).convertAndSend(eq("ex.thelook.outfit"), eq("image.status.updated"), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ITEM");
        verify(channel).basicAck(4L, false);
    }

    @Test
    void handleLowPriority_erro_naoProcessaENack() throws IOException {
        Map<String, Object> payload = buildPayload(UUID.randomUUID(), List.of("item/image.jpg"));
        when(imageWorkerService.processToWebp(anyString())).thenThrow(new RuntimeException("falha"));

        imageTaskListener.handleLowPriority(payload, channel, 5L);

        verify(channel).basicNack(5L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> buildPayload(UUID outfitId, List<String> images) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("outfitId", outfitId.toString());
        payload.put("images", images);
        return payload;
    }
}
