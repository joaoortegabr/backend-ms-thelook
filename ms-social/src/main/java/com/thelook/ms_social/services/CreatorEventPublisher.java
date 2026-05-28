package com.thelook.ms_social.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_social.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreatorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CreatorEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public CreatorEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishDeactivated(UUID creatorId) {
        publish(new CreatorLifecycleEventDTO("DEACTIVATED", creatorId, null));
    }

    public void publishReactivated(UUID creatorId) {
        publish(new CreatorLifecycleEventDTO("REACTIVATED", creatorId, null));
    }

    public void publishPurged(UUID creatorId, UUID userId) {
        publish(new CreatorLifecycleEventDTO("PURGED", creatorId, userId));
    }

    private void publish(CreatorLifecycleEventDTO event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CREATOR_EXCHANGE,
                    RabbitMQConfig.CREATOR_LIFECYCLE_KEY,
                    event);
            log.info("Evento {} publicado para creatorId={}", event.type(), event.creatorId());
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} para creatorId={}: {}", event.type(), event.creatorId(), e.getMessage(), e);
        }
    }
}