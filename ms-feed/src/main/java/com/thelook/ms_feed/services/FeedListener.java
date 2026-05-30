package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.OutfitDeletedDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.ms_feed.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FeedListener {

    private static final Logger log = LoggerFactory.getLogger(FeedListener.class);

    private final OutfitIndexService outfitIndexService;

    public FeedListener(OutfitIndexService outfitIndexService) {
        this.outfitIndexService = outfitIndexService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_FEED_SYNC, containerFactory = "feedContainerFactory")
    public void handleOutfitCreated(OutfitSyncDTO syncDTO,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Recebido outfit para sincronizacao: {}", syncDTO.outfitId());
        try {
            outfitIndexService.index(syncDTO);
            channel.basicAck(tag, false);
            log.info("Outfit adicionado com sucesso: {}", syncDTO.outfitId());
        } catch (Exception e) {
            log.error("Falha ao adicionar outfit {}: {}", syncDTO.outfitId(), e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_OUTFIT_DELETED, containerFactory = "feedContainerFactory")
    public void handleOutfitDeleted(OutfitDeletedDTO dto,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Removendo outfit do feed: {}", dto.outfitId());
        try {
            outfitIndexService.removeById(dto.outfitId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Falha ao remover outfit {} do feed: {}", dto.outfitId(), e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }

}
