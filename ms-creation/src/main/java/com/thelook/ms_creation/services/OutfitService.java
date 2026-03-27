package com.thelook.ms_creation.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;
import com.thelook.ms_creation.repositories.OutfitRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final StorageService storageService;

    public OutfitService(OutfitRepository outfitRepository, StorageService storageService) {
        this.outfitRepository = outfitRepository;
        this.storageService = storageService;
    }

    @Transactional
    public Outfit create(UUID creatorId, OutfitRequest request, MultipartFile file) {

        Outfit outfit = new Outfit();
        outfit.setCreatorId(creatorId);
        outfit.setTitle(request.title());
        outfit.setImage1Url(request.image1Url());
        outfit.setImage2Url(request.image2Url());
        outfit.setStyle(request.style());
        outfit.setColors(request.colors());

        if (request.items() != null) {
            request.items().forEach(itemRequest -> {
                Item item = new Item();
                item.setOutfit(itemRequest.outfit());
                item.setItemType(itemRequest.itemType());
                item.setItemName(itemRequest.itemName());
                item.setItemImg(itemRequest.itemName());
                item.setItemUrl(itemRequest.itemUrl());
                outfit.addItem(item);
            });
        }
        return outfitRepository.save(outfit);
    }

    @Transactional
    public void delete(UUID outfitId, UUID creatorId) {
        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() -> new ResourceNotFoundException("Outfit não encontrado"));

        // Segurança: Apenas o dono pode deletar
        if (!outfit.getCreatorId().equals(creatorId)) {
            throw new BusinessRuleException("Acesso negado para deletar este look");
        }

        outfitRepository.delete(outfit);
        // Próximo passo: disparar evento para deletar arquivo físico e nó no Neo4j
    }

}
