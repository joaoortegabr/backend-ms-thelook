package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CreatorLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(CreatorLifecycleListener.class);

    private final OutfitElasticRepository repository;

    public CreatorLifecycleListener(OutfitElasticRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "q.creator.lifecycle.feed", containerFactory = "lifecycleContainerFactory")
    public void handle(CreatorLifecycleEventDTO event,
                       Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Recebido evento {} para creatorId={}", event.type(), event.creatorId());
        try {
            switch (event.type()) {
                case "DEACTIVATED" -> setActive(event.creatorId().toString(), false);
                case "REACTIVATED" -> setActive(event.creatorId().toString(), true);
                case "PURGED"      -> repository.deleteByCreatorId(event.creatorId().toString());
                default -> log.warn("Tipo de evento desconhecido: {}", event.type());
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Falha ao processar evento {} para creatorId={}: {}", event.type(), event.creatorId(), e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }

    private void setActive(String creatorId, boolean active) {
        List<OutfitDocument> docs = repository.findByCreatorId(creatorId);
        docs.forEach(doc -> doc.setIsActive(active));
        repository.saveAll(docs);
        log.info("{} outfits do creator {} marcados como active={}", docs.size(), creatorId, active);
    }
}