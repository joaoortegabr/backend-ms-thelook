package com.thelook.ms_social.services;

import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.entities.CreatorNode;
import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class CreatorService {

    private final long CACHE_DURATION_DAYS = 90;

    private final CreatorRepository creatorRepository;
    private final CreatorNodeRepository creatorNodeRepository;
    private final StringRedisTemplate redisTemplate;

    public CreatorService(CreatorRepository creatorRepository, 
                          CreatorNodeRepository creatorNodeRepository,
                          StringRedisTemplate redisTemplate) {
        this.creatorRepository = creatorRepository;
        this.creatorNodeRepository = creatorNodeRepository;
        this.redisTemplate = redisTemplate;
    }

    public Creator findById(UUID creatorId) {
        return creatorRepository.findIdByCreatorId(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException(creatorId));
    }
    
    @Transactional
    public Creator create(UUID userId, CreatorRequest request) {

        Creator creator = new Creator(
                userId,
                request.name(),
                request.avatarUrl(),
                request.bio(),
                request.instagram(),
                request.birthDate(),
                request.city(),
                request.uf()
        );
        creator.setIsActive(true);
        creatorRepository.save(creator);

        creatorNodeRepository.save(new CreatorNode(creator.getId(), creator.getName()));

        String redisKey = "user:profile:" + userId;
        redisTemplate.opsForValue().set(
                redisKey,
                creator.getId().toString(),
                Duration.ofDays(CACHE_DURATION_DAYS)
        );

        return creator;
    }

    @Transactional
    public Creator update(UUID creatorId, CreatorUpdateRequest request) {

        Creator creator = findById(creatorId);

        if (request.avatarUrl() != null)
            creator.setAvatarUrl(request.avatarUrl());
        if (request.bio() != null)
            creator.setBio(request.bio());
        if (request.instagram() != null)
            creator.setInstagram(request.instagram());
        if (request.city() != null)
            creator.setCity(request.city());
        if (request.uf() != null)
            creator.setUf(request.uf());

        creatorRepository.save(creator);
        return creator;
    }

    @Transactional
    public String delete(UUID creatorId) {

        if (!creatorRepository.existsById(creatorId))
            throw new ResourceNotFoundException("Creator not found");

        creatorRepository.deleteById(creatorId);
        creatorNodeRepository.deepDeleteCreator(creatorId);
        return "Registro removido com sucesso.";
    }

}