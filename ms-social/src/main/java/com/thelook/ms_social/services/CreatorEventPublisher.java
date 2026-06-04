package com.thelook.ms_social.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.ms_social.entities.OutboxMessage;
import com.thelook.ms_social.repositories.OutboxRepository;
import com.thelook.ms_social.services.events.OutboxSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreatorEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CreatorEventPublisher.class);

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public CreatorEventPublisher(OutboxRepository outboxRepository,
                                  ObjectMapper objectMapper,
                                  ApplicationEventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public void publishDeactivated(UUID creatorId) {
        saveToOutbox("CREATOR_DEACTIVATED", creatorId, null);
    }

    public void publishReactivated(UUID creatorId) {
        saveToOutbox("CREATOR_REACTIVATED", creatorId, null);
    }

    public void publishPurged(UUID creatorId, UUID userId) {
        saveToOutbox("CREATOR_PURGED", creatorId, userId);
    }

    private void saveToOutbox(String outboxType, UUID creatorId, UUID userId) {
        String lifecycleType = outboxType.replace("CREATOR_", "");
        CreatorLifecycleEventDTO event = new CreatorLifecycleEventDTO(lifecycleType, creatorId, userId);

        OutboxMessage message = new OutboxMessage();
        message.setAggregateId(creatorId.toString());
        message.setType(outboxType);
        try {
            message.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Erro ao serializar evento de creator: " + e.getMessage());
        }

        outboxRepository.save(message);
        eventPublisher.publishEvent(new OutboxSavedEvent());
        log.debug("Evento {} gravado no outbox para creatorId={}", outboxType, creatorId);
    }
}