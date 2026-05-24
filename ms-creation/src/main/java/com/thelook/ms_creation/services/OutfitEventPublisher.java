package com.thelook.ms_creation.services;

import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_creation.config.RabbitMQConfig;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OutfitEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final OutfitMapper outfitMapper;
    private final String EXCHANGE = RabbitMQConfig.OUTFIT_EXCHANGE;

    public OutfitEventPublisher(RabbitTemplate rabbitTemplate, OutfitMapper outfitMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.outfitMapper = outfitMapper;
    }

    public void publishOutfitCreated(Outfit outfit) {
        // 1. Enviar para Sincronização do Feed (Objeto Completo)
        // O ms-feed usará isso para criar o documento no Elasticsearch
        OutfitSyncDTO syncDTO = convertToSyncDTO(outfit);
        rabbitTemplate.convertAndSend(EXCHANGE, "feed.sync.created", syncDTO);

        // 2. Enviar Imagens do Outfit para Alta Prioridade (Compactação)
        Map<String, Object> highPriorityPayload = Map.of(
                "outfitId", outfit.getId(),
                "images", List.of(outfit.getImage1Url(), outfit.getImage2Url() != null ? outfit.getImage2Url() : "")
        );
        rabbitTemplate.convertAndSend(EXCHANGE, "image.high.process", highPriorityPayload);

        // 3. Enviar Imagens dos Itens para Baixa Prioridade
        if (!outfit.getItems().isEmpty()) {
            java.util.List<String> itemImagePaths = outfit.getItems().stream()
                    .map(Item::getItemImg)
                    .toList();

            Map<String, Object> lowPriorityPayload = Map.of(
                    "outfitId", outfit.getId(),
                    "itemImages", itemImagePaths
            );
            rabbitTemplate.convertAndSend(EXCHANGE, "image.low.process", lowPriorityPayload);
        }
    }

    private OutfitSyncDTO convertToSyncDTO(Outfit outfit) {
        return outfitMapper.toOutfitSyncDTO(outfit);
    }
}