package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.repositories.CreatorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class CreatorService {

    private static final Logger log = LoggerFactory.getLogger(CreatorService.class);
    private final long CACHE_DURATION_DAYS = 90;

    private final CreatorNodeService creatorNodeService;
    private final CreatorRepository creatorRepository;
    private final StringRedisTemplate redisTemplate;

    public CreatorService(CreatorNodeService creatorNodeService,
                          CreatorRepository creatorRepository,
                          StringRedisTemplate redisTemplate) {
        this.creatorNodeService = creatorNodeService;
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
    }

    public Creator findById(UUID creatorId) {
        return creatorRepository.findIdByCreatorId(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException(creatorId));
    }

    @Transactional(transactionManager = "transactionManager")
    public Creator create(UUID userId, CreatorRequest request) {

        Creator creator;
        try {
            creator = new Creator(
                    userId,
                    request.name(),
                    request.avatarUrl(),
                    request.bio(),
                    request.instagram(),
                    request.birthDate(),
                    request.city(),
                    request.uf());
            creator.setIsActive(true);
            creatorRepository.save(creator);
        } catch(RuntimeException e) {
            throw new BusinessRuleException("Username already associated to another account: " + e.getMessage());
        }

        saveCreatorToNeo4j(creator);
        redisTemplate.opsForValue().set(
                "user:profile:" + userId,
                creator.getId().toString(),
                Duration.ofDays(CACHE_DURATION_DAYS)
        );
        log.info("/////New Creator registered={},{},{}/{}",
                request.name(), request.instagram(), request.city(), request.uf());
        return creator;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    private void saveCreatorToNeo4j(Creator creator) {
        creatorNodeService.save(creator);
    }

    @Transactional(transactionManager = "transactionManager")
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

    @Transactional(transactionManager = "transactionManager")
    public String delete(UUID creatorId) {
        try {
            creatorRepository.deleteById(creatorId);
            deleteCreatorFromNeo4j(creatorId);
            return "Registro removido com sucesso.";
        } catch(Exception e) {
            throw new ResourceNotFoundException("Error deleting register: " + e.getMessage());
        }
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    private void deleteCreatorFromNeo4j(UUID creatorId) {
        creatorNodeService.delete(creatorId);
    }

}