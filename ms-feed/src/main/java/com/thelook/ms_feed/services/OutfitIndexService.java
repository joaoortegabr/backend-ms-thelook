package com.thelook.ms_feed.services;

import com.thelook.dtos.ItemSyncDTO;
import com.thelook.dtos.OutfitSyncDTO;
import java.util.UUID;
import com.thelook.ms_feed.entities.ItemDocument;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutfitIndexService {

    private static final Logger log = LoggerFactory.getLogger(OutfitIndexService.class);

    private final OutfitElasticRepository repository;

    public OutfitIndexService(OutfitElasticRepository repository) {
        this.repository = repository;
    }

    public void index(OutfitSyncDTO dto) {
        OutfitDocument doc = new OutfitDocument();
        doc.setId(dto.outfitId().toString());
        doc.setCreatorId(dto.creatorId());
        doc.setTitle(dto.title());
        doc.setStyle(dto.style());
        doc.setColors(dto.colors());
        doc.setImage1Url(dto.image1Url());
        doc.setImage2Url(dto.image2Url());
        doc.setImageStatus(dto.imageStatus().name());
        doc.setCreatedAt(LocalDateTime.now());

        if (dto.items() != null) {
            List<ItemDocument> itemDocs = dto.items().stream()
                    .map(this::mapItemToDocument)
                    .toList();
            doc.setItems(itemDocs);
        }

        repository.save(doc);
        log.debug("Outfit {} salvo no indice com {} itens", dto.outfitId(), doc.getItems().size());
    }

    public void removeById(UUID outfitId) {
        repository.deleteById(outfitId.toString());
        log.info("Outfit {} removido do indice", outfitId);
    }

    private ItemDocument mapItemToDocument(ItemSyncDTO itemDto) {
        ItemDocument itemDoc = new ItemDocument();
        itemDoc.setItemId(itemDto.itemId().toString());
        itemDoc.setItemName(itemDto.itemName());
        itemDoc.setItemType(itemDto.itemType());
        itemDoc.setItemImg(itemDto.itemImg());
        itemDoc.setItemUrl(itemDto.itemUrl());
        itemDoc.setImageStatus(itemDto.imageStatus().name());
        return itemDoc;
    }

}
