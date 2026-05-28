package com.thelook.ms_creation.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreatorLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(CreatorLifecycleListener.class);

    private final OutfitRepository outfitRepository;
    private final StorageService storageService;

    public CreatorLifecycleListener(OutfitRepository outfitRepository, StorageService storageService) {
        this.outfitRepository = outfitRepository;
        this.storageService = storageService;
    }

    @RabbitListener(queues = "q.creator.lifecycle.creation")
    @Transactional
    public void handle(CreatorLifecycleEventDTO event) {
        log.info("Recebido evento {} para creatorId={}", event.type(), event.creatorId());
        try {
            switch (event.type()) {
                case "DEACTIVATED" -> outfitRepository.deactivateByCreatorId(event.creatorId());
                case "REACTIVATED" -> outfitRepository.reactivateByCreatorId(event.creatorId());
                case "PURGED"      -> purge(event);
                default -> log.warn("Tipo de evento desconhecido: {}", event.type());
            }
        } catch (Exception e) {
            log.error("Falha ao processar evento {} para creatorId={}: {}", event.type(), event.creatorId(), e.getMessage(), e);
            throw e;
        }
    }

    private void purge(CreatorLifecycleEventDTO event) {
        List<Outfit> outfits = outfitRepository.findAllWithItemsByCreatorId(event.creatorId());

        for (Outfit outfit : outfits) {
            storageService.deleteImage(outfit.getImage1Url());
            storageService.deleteImage(outfit.getImage2Url());
            outfit.getItems().forEach(item -> storageService.deleteImage(item.getItemImg()));
        }

        outfitRepository.deleteAll(outfits);
        log.info("{} outfits do creator {} removidos permanentemente", outfits.size(), event.creatorId());
    }
}