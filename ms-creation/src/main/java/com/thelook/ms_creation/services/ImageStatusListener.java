package com.thelook.ms_creation.services;

import com.thelook.dtos.ImageProcessedDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ImageStatusListener {

    private final OutfitRepository repository;

    public ImageStatusListener(OutfitRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "q.image.status.updated.ms-creation")
    public void handleImageProcessed(ImageProcessedDTO dto) {
        repository.findById(dto.outfitId()).ifPresent(outfit -> {
            if ("OUTFIT".equals(dto.type())) {
                outfit.setImage1Url(dto.processedPath());
                outfit.setImageStatus(ImageProcessStatus.READY);
            }
            // Adicione a lógica para imagem2 ou itens conforme necessário
            repository.save(outfit);
        });
    }

}
