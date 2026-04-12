package com.thelook.ms_feed.services;

import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class FeedUpdateListener {

    private final OutfitElasticRepository elasticRepository;

    public FeedUpdateListener(OutfitElasticRepository elasticRepository) {
        this.elasticRepository = elasticRepository;
    }

    @RabbitListener(queues = "q.image.status.updated.ms-feed")
    public void handleFeedImageUpdate(ImageProcessedDTO dto) {
        elasticRepository.findById(dto.outfitId().toString()).ifPresent(doc -> {
            if ("MAIN".equals(dto.type())) {
                doc.setImage1Url(dto.processedPath());
                doc.setImageStatus("READY");
            }
            elasticRepository.save(doc);
        });
    }

}
