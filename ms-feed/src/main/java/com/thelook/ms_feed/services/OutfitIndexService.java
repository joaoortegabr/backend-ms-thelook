package com.thelook.ms_feed.services;

import com.thelook.dtos.ItemSyncDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_feed.entities.ItemDocument;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OutfitIndexService {

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
        doc.setCreatedAt(LocalDateTime.now()); // Ou extraia do DTO se enviou

        if (dto.items() != null) {
            List<ItemDocument> itemDocs = dto.items().stream().map(itemDto -> {
                ItemDocument itemDoc = new ItemDocument();
                itemDoc.setItemId(itemDto.itemId().toString());
                itemDoc.setItemName(itemDto.itemName());
                itemDoc.setItemType(itemDto.itemType());
                itemDoc.setItemImg(itemDto.itemImg());
                itemDoc.setItemUrl(itemDto.itemUrl());
                itemDoc.setImageStatus(itemDto.imageStatus().name());
                return itemDoc;
            }).toList();
            doc.setItems(itemDocs);
        }

        // Graças ao Java 21, essa chamada HTTP para o ES é "non-blocking" para a CPU
        repository.save(doc);
    }

    private OutfitDocument convertToDocument(OutfitSyncDTO dto) {
        OutfitDocument doc = new OutfitDocument();

        doc.setId(dto.outfitId().toString());
        doc.setCreatorId(dto.creatorId());
        doc.setTitle(dto.title());
        doc.setStyle(dto.style());
        doc.setColors(dto.colors());
        doc.setImage1Url(dto.image1Url());
        doc.setImage2Url(dto.image2Url());
        doc.setImageStatus(dto.imageStatus().name());

        // O createdAt pode vir do DTO ou ser gerado agora se for um novo registro
        doc.setCreatedAt(LocalDateTime.now());

        // Mapeamento da lista de itens (Nested)
        if (dto.items() != null) {
            List<ItemDocument> itemDocs = dto.items().stream()
                    .map(this::mapItemToDocument)
                    .toList();
            doc.setItems(itemDocs);
        }

        return doc;
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
