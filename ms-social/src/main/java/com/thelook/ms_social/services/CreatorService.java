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
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CreatorService {

    private static final Logger log = LoggerFactory.getLogger(CreatorService.class);
    private static final long CACHE_DURATION_DAYS = 90;

    private final CreatorNodeService creatorNodeService;
    private final CreatorRepository creatorRepository;
    private final StringRedisTemplate redisTemplate;
    private final CreatorEventPublisher creatorEventPublisher;

    public CreatorService(CreatorNodeService creatorNodeService,
                          CreatorRepository creatorRepository,
                          StringRedisTemplate redisTemplate,
                          CreatorEventPublisher creatorEventPublisher) {
        this.creatorNodeService = creatorNodeService;
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
        this.creatorEventPublisher = creatorEventPublisher;
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
        } catch (RuntimeException e) {
            throw new BusinessRuleException("Username already associated to another account");
        }

        saveCreatorToNeo4j(creator);
        redisTemplate.opsForValue().set(
                "user:profile:" + userId,
                creator.getId().toString(),
                Duration.ofDays(CACHE_DURATION_DAYS)
        );
        log.info("Novo Creator registrado: nome={}, instagram={}, cidade={}/{}",
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

        if (request.avatarUrl() != null) creator.setAvatarUrl(request.avatarUrl());
        if (request.bio() != null) creator.setBio(request.bio());
        if (request.instagram() != null) creator.setInstagram(request.instagram());
        if (request.city() != null) creator.setCity(request.city());
        if (request.uf() != null) creator.setUf(request.uf());

        creatorRepository.save(creator);
        redisTemplate.delete("user:profile:" + creator.getUserId());

        return creator;
    }

    @Transactional(transactionManager = "transactionManager")
    public String delete(UUID creatorId) {
        Creator creator = findById(creatorId);

        creator.setIsActive(false);
        creator.setDeletedAt(LocalDateTime.now());
        creatorRepository.save(creator);

        redisTemplate.delete("user:profile:" + creator.getUserId());
        redisTemplate.delete("followers:count:" + creatorId);

        deleteCreatorFromNeo4j(creatorId);
        creatorEventPublisher.publishDeactivated(creatorId);
        log.info("Creator {} desativado (soft delete). Recuperavel em ate 90 dias.", creatorId);
        return "Conta desativada. Voce tem 90 dias para recupera-la.";
    }

    @Transactional(transactionManager = "transactionManager")
    public Creator reactivate(UUID userId) {
        Creator creator = creatorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Creator nao encontrado para este usuario"));

        if (creator.getIsActive()) {
            throw new BusinessRuleException("Esta conta ja esta ativa");
        }

        if (creator.getDeletedAt() != null &&
                creator.getDeletedAt().isBefore(LocalDateTime.now().minusDays(90))) {
            throw new BusinessRuleException("Prazo de recuperacao expirado. A conta foi permanentemente encerrada");
        }

        creator.setIsActive(true);
        creator.setDeletedAt(null);
        creatorRepository.save(creator);

        saveCreatorToNeo4j(creator);
        redisTemplate.opsForValue().set(
                "user:profile:" + userId,
                creator.getId().toString(),
                Duration.ofDays(CACHE_DURATION_DAYS)
        );

        creatorEventPublisher.publishReactivated(creator.getId());
        log.info("Creator {} reativado pelo usuario {}", creator.getId(), userId);
        return creator;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    private void deleteCreatorFromNeo4j(UUID creatorId) {
        creatorNodeService.delete(creatorId);
    }
}