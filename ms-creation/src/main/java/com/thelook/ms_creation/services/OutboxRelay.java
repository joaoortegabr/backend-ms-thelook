package com.thelook.ms_creation.services;

import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.repositories.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void publishMessages() {
        List<OutboxMessage> messages = outboxRepository.findByProcessedFalse();

        for (OutboxMessage message : messages) {
            try {
                rabbitTemplate.convertAndSend("ex.thelook.outfit", "outfit.created", message.getPayload());

                message.setProcessed(true);
                outboxRepository.save(message);
            } catch (Exception e) {
                log.error("Falha ao enviar mensagem do Outbox: " + message.getId());
            }
        }
    }
}
