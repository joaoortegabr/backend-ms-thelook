package com.thelook.ms_worker.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.enums.ImageProcessStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ImageTaskListener {

    private final ImageWorkerService imageWorkerService;
    private final RabbitTemplate rabbitTemplate;

    public ImageTaskListener(ImageWorkerService imageWorkerService, RabbitTemplate rabbitTemplate) {
        this.imageWorkerService = imageWorkerService;
        this.rabbitTemplate = rabbitTemplate;
    }

    // Unificamos a lógica aqui. Remova os métodos receiveLowPriority antigos.
    @RabbitListener(queues = "q.image.process.high", ackMode = "MANUAL", containerFactory = "highPriorityContainerFactory")
    public void handleHighPriority(Map<String, Object> payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            System.out.println("Processando imagem de ALTA prioridade...");
            processTask(payload, "OUTFIT");
            channel.basicAck(tag, false); // Confirma que processou com sucesso
        } catch (Exception e) {
            System.err.println("Erro no processamento HIGH: " + e.getMessage());
            channel.basicNack(tag, false, false); // Devolve para a fila se falhar
        }
    }

    @RabbitListener(queues = "q.image.process.low", ackMode = "MANUAL", containerFactory = "lowPriorityContainerFactory")
    public void handleLowPriority(Map<String, Object> payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            System.out.println("Processando imagem de baixa prioridade...");
            processTask(payload, "ITEM");
            channel.basicAck(tag, false);
        } catch (Exception e) {
            System.err.println("Erro no processamento LOW: " + e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }

    private void processTask(Map<String, Object> payload, String type) {
        // Usar Object -> String -> UUID é mais seguro para evitar ClassCastException
        UUID outfitId = UUID.fromString(payload.get("outfitId").toString());
        List<String> paths = (List<String>) payload.get("images");

        if (paths == null) return;

        for (String originalPath : paths) {
            if (originalPath != null && !originalPath.isEmpty()) {
                // Conversão para WebP
                String processedPath = imageWorkerService.processToWebp(originalPath);

                // Notifica ms-creation e ms-feed através da Topic Exchange
                ImageProcessedDTO response = new ImageProcessedDTO(
                        outfitId,
                        originalPath,
                        processedPath,
                        type,
                        ImageProcessStatus.READY
                );

                rabbitTemplate.convertAndSend("ex.thelook.outfit", "image.status.updated", response);
            }
        }
    }
}