package com.thelook.ms_creation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.dtos.OutfitDeletedDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.repositories.OutboxRepository;
import com.thelook.ms_creation.repositories.OutfitRepository;
import com.thelook.ms_creation.services.events.OutboxSavedEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final OutfitMapper outfitMapper;
    private final ApplicationEventPublisher eventPublisher;

    public OutfitService(OutfitRepository outfitRepository, OutboxRepository outboxRepository,
                         ObjectMapper objectMapper, StorageService storageService,
                         OutfitMapper outfitMapper, ApplicationEventPublisher eventPublisher) {
        this.outfitRepository = outfitRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.outfitMapper = outfitMapper;
        this.eventPublisher = eventPublisher;
    }

    public Outfit findById(UUID outfitId) {
        return outfitRepository.findWithItemsById(outfitId)
                .orElseThrow(() -> new ResourceNotFoundException(outfitId));
    }

    @Transactional
    public Outfit createOutfit(UUID creatorId, OutfitRequest request,
                                   MultipartFile mainImage,
                                   MultipartFile secondImage,
                                   List<MultipartFile> itemImages) {

        UUID outfitId = UUID.randomUUID();
        Outfit outfit = new Outfit();
        outfit.setId(outfitId);
        outfit.setCreatorId(creatorId);
        outfit.setTitle(request.title());
        outfit.setStyle(request.style());
        outfit.setColors(request.colors());
        outfit.setImageStatus(ImageProcessStatus.PENDING);

        String mainPath = storageService.saveImage(mainImage, creatorId, outfitId, "main_look_1");
        outfit.setImage1Url(mainPath);

        if (secondImage != null && !secondImage.isEmpty()) {
            String secondPath = storageService.saveImage(secondImage, creatorId, outfitId, "main_look_2");
            outfit.setImage2Url(secondPath);
        }

        if (request.items() != null && !request.items().isEmpty()) {
            for (int i = 0; i < request.items().size(); i++) {
                ItemRequest itemReq = request.items().get(i);
                Item item = new Item();
                item.setItemType(itemReq.itemType());
                item.setItemName(itemReq.itemName());
                item.setItemUrl(itemReq.itemUrl());
                item.setImageStatus(ImageProcessStatus.PENDING);

                if (itemImages != null && i < itemImages.size()) {
                    MultipartFile itemFile = itemImages.get(i);
                    if (itemFile != null && !itemFile.isEmpty()) {
                        String itemPath = storageService.saveImage(itemFile, creatorId, outfitId, "item_" + i);
                        item.setItemImg(itemPath);
                    }
                }

                item.setOutfit(outfit);
                outfit.getItems().add(item);
            }
        }

        Outfit saved = outfitRepository.save(outfit);
        OutfitSyncDTO syncDto = outfitMapper.toOutfitSyncDTO(saved);
        OutboxMessage message = new OutboxMessage();
        try {
            message.setAggregateId(saved.getId().toString());
            message.setType("OUTFIT_CREATED");
            message.setPayload(objectMapper.writeValueAsString(syncDto));
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Erro ao serializar evento do outfit: " + e.getMessage());
        }

        outboxRepository.save(message);
        eventPublisher.publishEvent(new OutboxSavedEvent());
        return saved;
    }

    @Transactional
    public void delete(UUID outfitId, UUID creatorId) {
        Outfit outfit = outfitRepository.findWithItemsById(outfitId)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit não encontrado"));

        if (!outfit.getCreatorId().equals(creatorId)) {
            throw new BusinessRuleException("Acesso negado para deletar este look");
        }

        outfit.setIsActive(false);
        outfit.setDeletedAt(java.time.LocalDateTime.now());
        outfitRepository.save(outfit);

        publishDeletedEvent(outfitId, creatorId);
    }

    @Transactional
    public void hardDelete(UUID outfitId, UUID creatorId) {
        Outfit outfit = outfitRepository.findWithItemsById(outfitId)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit não encontrado"));

        if (!outfit.getCreatorId().equals(creatorId)) {
            throw new BusinessRuleException("Acesso negado para deletar este look");
        }

        storageService.deleteImage(outfit.getImage1Url());
        storageService.deleteImage(outfit.getImage2Url());
        outfit.getItems().forEach(item -> storageService.deleteImage(item.getItemImg()));

        outfitRepository.delete(outfit);

        publishDeletedEvent(outfitId, creatorId);
    }

    private void publishDeletedEvent(UUID outfitId, UUID creatorId) {
        OutboxMessage deleteEvent = new OutboxMessage();
        try {
            deleteEvent.setAggregateId(outfitId.toString());
            deleteEvent.setType("OUTFIT_DELETED");
            deleteEvent.setPayload(objectMapper.writeValueAsString(new OutfitDeletedDTO(outfitId, creatorId)));
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Erro ao serializar evento de deleção: " + e.getMessage());
        }
        outboxRepository.save(deleteEvent);
        eventPublisher.publishEvent(new OutboxSavedEvent());
    }

}
