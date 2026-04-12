package com.thelook.ms_feed.services;

import com.thelook.dtos.OutfitSyncDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class FeedListener {

    private final OutfitIndexService outfitIndexService;

    public FeedListener(OutfitIndexService outfitIndexService) {
        this.outfitIndexService = outfitIndexService;
    }

    @RabbitListener(queues = "q.feed.sync")
    public void handleOutfitCreated(OutfitSyncDTO syncDTO) {
        System.out.println("Recebido Outfit para sincronizacao: " + syncDTO.outfitId());
        outfitIndexService.index(syncDTO);
    }

}
