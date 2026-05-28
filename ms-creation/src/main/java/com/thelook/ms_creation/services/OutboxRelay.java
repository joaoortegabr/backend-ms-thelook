package com.thelook.ms_creation.services;

import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.repositories.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxRelay {

    private static final long OUTBOX_RELAY_DELAY_MS = 1000;

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay=OUTBOX_RELAY_DELAY_MS)
    @Transactional
    public void publishMessages() {
        Pageable page = PageRequest.of(0, 100);
        List<OutboxMessage> messages = outboxRepository.findByProcessedFalse(page);
        List<UUID> processedIds = new ArrayList<>();

        for (OutboxMessage message : messages) {
            String routingKey = switch (message.getType()) {
                case "OUTFIT_CREATED", "OUTFIT_UPDATED" -> "feed.sync";
                case "OUTFIT_DELETED" -> "outfit.deleted";
                default -> {
                    log.warn("Tipo desconhecido no Outbox: {} (id={})", message.getType(), message.getId());
                    yield null;
                }
            };

            if (routingKey == null) {
                processedIds.add(message.getId()); // evita reprocessamento infinito de tipos inválidos
                continue;
            }

            try {
                rabbitTemplate.convertAndSend("ex.thelook.outfit", routingKey, message.getPayload());
                processedIds.add(message.getId());
            } catch (Exception e) {
                log.error("Falha ao enviar mensagem do Outbox {}: {}", message.getId(), e.getMessage(), e);
            }
        }

        if (!processedIds.isEmpty()) {
            outboxRepository.markAsProcessed(processedIds);
        }
    }
}
