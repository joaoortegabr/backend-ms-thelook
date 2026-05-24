package com.thelook.ms_feed.services;

import com.thelook.dtos.OutfitSyncDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class FeedListener {

    private static final Logger log = LoggerFactory.getLogger(FeedListener.class);

    private final OutfitIndexService outfitIndexService;

    public FeedListener(OutfitIndexService outfitIndexService) {
        this.outfitIndexService = outfitIndexService;
    }

    @RabbitListener(queues = "q.feed.sync")
    public void handleOutfitCreated(OutfitSyncDTO syncDTO) {
        log.info("Recebido outfit para sincronizacao: {}", syncDTO.outfitId());
        try {
            outfitIndexService.index(syncDTO);
            log.info("Outfit adicionado com sucesso: {}", syncDTO.outfitId());
        } catch (Exception e) {
            log.error("Falha ao adicionar outfit {}: {}", syncDTO.outfitId(), e.getMessage(), e);
            throw e;
        }
    }

}
