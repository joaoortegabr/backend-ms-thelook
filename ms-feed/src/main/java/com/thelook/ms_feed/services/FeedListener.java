package com.thelook.ms_feed.services;

import com.thelook.dtos.OutfitDeletedDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_feed.config.RabbitMQConfig;
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_FEED_SYNC)
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_OUTFIT_DELETED)
    public void handleOutfitDeleted(OutfitDeletedDTO dto) {
        log.info("Removendo outfit do feed: {}", dto.outfitId());
        try {
            outfitIndexService.removeById(dto.outfitId());
        } catch (Exception e) {
            log.error("Falha ao remover outfit {} do feed: {}", dto.outfitId(), e.getMessage(), e);
            throw e;
        }
    }

}
