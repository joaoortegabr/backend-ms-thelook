package com.thelook.ms_creation.services;

import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ImageStatusListener {

    private static final Logger log = LoggerFactory.getLogger(ImageStatusListener.class);

    private final OutfitRepository repository;

    public ImageStatusListener(OutfitRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "q.image.status.updated.ms-creation")
    public void handleImageProcessed(ImageProcessedDTO dto) {
        log.info("Recebido update de imagem para outfit {}: tipo={}", dto.outfitId(), dto.type());
        repository.findById(dto.outfitId()).ifPresentOrElse(outfit -> {
            if ("OUTFIT".equals(dto.type())) {
                outfit.setImage1Url(dto.processedPath());
                outfit.setImageStatus(ImageProcessStatus.READY);
            } else if ("SECONDARY".equals(dto.type())) {
                outfit.setImage2Url(dto.processedPath());
            } else if ("ITEM".equals(dto.type())) {
                outfit.getItems().stream()
                        .filter(item -> dto.originalPath().equals(item.getItemImg()))
                        .findFirst()
                        .ifPresent(item -> {
                            item.setItemImg(dto.processedPath());
                            item.setImageStatus(ImageProcessStatus.READY);
                        });
            } else {
                log.warn("Tipo de imagem desconhecido '{}' para outfit {}", dto.type(), dto.outfitId());
            }
            repository.save(outfit);
            log.info("Outfit {} atualizado com imagem tipo={}", dto.outfitId(), dto.type());
        }, () -> log.warn("Outfit {} nao encontrado para update de imagem", dto.outfitId()));
    }

}
