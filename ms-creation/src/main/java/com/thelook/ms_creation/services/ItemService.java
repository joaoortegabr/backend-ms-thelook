package com.thelook.ms_creation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import com.thelook.ms_creation.models.mappers.ItemMapper;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.repositories.ItemRepository;
import com.thelook.ms_creation.repositories.OutboxRepository;
import com.thelook.ms_creation.repositories.OutfitRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final OutfitRepository outfitRepository;
    private final OutboxRepository outboxRepository;
    private final StorageService storageService;
    private final ItemMapper itemMapper;
    private final OutfitMapper outfitMapper;
    private final ObjectMapper objectMapper;

    public ItemService(ItemRepository itemRepository,
                       OutfitRepository outfitRepository,
                       OutboxRepository outboxRepository,
                       StorageService storageService,
                       ItemMapper itemMapper,
                       OutfitMapper outfitMapper,
                       ObjectMapper objectMapper) {
        this.itemRepository = itemRepository;
        this.outfitRepository = outfitRepository;
        this.outboxRepository = outboxRepository;
        this.storageService = storageService;
        this.itemMapper = itemMapper;
        this.outfitMapper = outfitMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ItemResponse addItem(UUID outfitId, UUID creatorId,
                                ItemRequest request, MultipartFile image) {
        Outfit outfit = outfitRepository.findWithItemsById(outfitId)
                .orElseThrow(() -> new ResourceNotFoundException(outfitId));

        if (!outfit.getCreatorId().equals(creatorId))
            throw new BusinessRuleException("Acesso negado para modificar este look");

        if (outfit.getItems().size() >= 6)
            throw new BusinessRuleException("Limite de 6 itens por look atingido");

        Item item = new Item();
        item.setItemType(request.itemType());
        item.setItemName(request.itemName());
        item.setItemUrl(request.itemUrl());
        item.setImageStatus(ImageProcessStatus.PENDING);

        if (image != null && !image.isEmpty()) {
            String path = storageService.saveImage(image, creatorId, outfitId,
                    "item_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            item.setItemImg(path);
        } else {
            item.setItemImg("");
        }

        outfit.addItem(item);
        outfitRepository.save(outfit);

        publishOutfitUpdated(outfit);

        return itemMapper.toItemResponse(item);
    }

    @Transactional
    public void deleteItem(UUID outfitId, UUID itemId, UUID creatorId) {
        Item item = itemRepository.findWithOutfitById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(itemId));

        Outfit outfit = item.getOutfit();

        if (!outfit.getId().equals(outfitId))
            throw new BusinessRuleException("Item não pertence a este look");

        if (!outfit.getCreatorId().equals(creatorId))
            throw new BusinessRuleException("Acesso negado para modificar este look");

        outfit.getItems().remove(item);
        outfitRepository.save(outfit);

        publishOutfitUpdated(outfit);
    }

    private void publishOutfitUpdated(Outfit outfit) {
        OutfitSyncDTO syncDto = outfitMapper.toOutfitSyncDTO(outfit);
        OutboxMessage message = new OutboxMessage();
        try {
            message.setAggregateId(outfit.getId().toString());
            message.setType("OUTFIT_UPDATED");
            message.setPayload(objectMapper.writeValueAsString(syncDto));
        } catch (JsonProcessingException e) {
            throw new BusinessRuleException("Erro ao serializar evento de atualizacao: " + e.getMessage());
        }
        outboxRepository.save(message);
    }
}