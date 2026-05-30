package com.thelook.ms_feed.services;

import com.rabbitmq.client.Channel;
import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FeedUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(FeedUpdateListener.class);

    private final OutfitElasticRepository elasticRepository;

    public FeedUpdateListener(OutfitElasticRepository elasticRepository) {
        this.elasticRepository = elasticRepository;
    }

    @RabbitListener(queues = "q.image.status.updated.ms-feed", containerFactory = "feedContainerFactory")
    public void handleFeedImageUpdate(ImageProcessedDTO dto,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("Recebido update de imagem para outfit {}: tipo={}", dto.outfitId(), dto.type());
        try {
            elasticRepository.findById(dto.outfitId().toString()).ifPresentOrElse(doc -> {
                if ("MAIN".equals(dto.type())) {
                    doc.setImage1Url(dto.processedPath());
                    doc.setImageStatus("READY");
                } else if ("SECONDARY".equals(dto.type())) {
                    doc.setImage2Url(dto.processedPath());
                } else {
                    log.warn("Tipo de imagem desconhecido '{}' para outfit {}", dto.type(), dto.outfitId());
                }
                elasticRepository.save(doc);
                log.info("Outfit {} atualizado no indice", dto.outfitId());
            }, () -> log.warn("Outfit {} nao encontrado no indice para update de imagem", dto.outfitId()));
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("Falha ao atualizar imagem do outfit {}: {}", dto.outfitId(), e.getMessage(), e);
            channel.basicNack(tag, false, false);
        }
    }

}
