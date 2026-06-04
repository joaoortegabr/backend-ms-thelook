package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class SocialService {

    private static final Logger log = LoggerFactory.getLogger(SocialService.class);

    private final CreatorNodeRepository creatorNodeRepository;
    private final StringRedisTemplate redisTemplate;

    public SocialService(CreatorNodeRepository creatorNodeRepository,
                         StringRedisTemplate redisTemplate) {
        this.creatorNodeRepository = creatorNodeRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void follow(UUID creatorId, UUID targetId) {

        if (creatorId.equals(targetId))
            throw new BusinessRuleException("Invalid request: you can't follow your own profile");

        if (creatorNodeRepository.isFollowing(creatorId, targetId)) {
            log.debug("Creator {} ja segue {}, ignorando", creatorId, targetId);
            return;
        }

        creatorNodeRepository.follow(creatorId, targetId);
        safeIncrement("followers:count:" + targetId);
        log.info("Creator {} seguiu {}. Contador de seguidores de {} incrementado", creatorId, targetId, targetId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void unfollow(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFollowing(creatorId, targetId)) {
            log.debug("Creator {} nao segue {}, ignorando unfollow", creatorId, targetId);
            return;
        }

        creatorNodeRepository.unfollow(creatorId, targetId);
        safeDecrement("followers:count:" + targetId);
        log.info("Creator {} deixou de seguir {}. Contador decrementado", creatorId, targetId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void favorite(UUID creatorId, UUID targetId) {

        if (creatorId.equals(targetId))
            throw new BusinessRuleException("You can't favorite your own profile");

        if (creatorNodeRepository.isFavorite(creatorId, targetId)) {
            log.debug("Creator {} ja favoritou {}, ignorando", creatorId, targetId);
            return;
        }

        creatorNodeRepository.favorite(creatorId, targetId);
        log.info("Creator {} favoritou {}", creatorId, targetId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void unfavorite(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFavorite(creatorId, targetId)) {
            log.debug("Creator {} nao favoritou {}, ignorando unfavorite", creatorId, targetId);
            return;
        }

        creatorNodeRepository.unfavorite(creatorId, targetId);
        log.info("Creator {} desfavoritou {}", creatorId, targetId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void likeOutfit(UUID creatorId, UUID outfitId) {

        if (creatorNodeRepository.isLiking(creatorId, outfitId)) {
            log.debug("Creator {} ja curtiu outfit {}, ignorando", creatorId, outfitId);
            return;
        }

        creatorNodeRepository.like(creatorId, outfitId);
        safeIncrement("likes:count:" + outfitId);
        log.info("Creator {} curtiu outfit {}. Contador incrementado", creatorId, outfitId);
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    public void unlikeOutfit(UUID creatorId, UUID outfitId) {

        if (!creatorNodeRepository.isLiking(creatorId, outfitId)) {
            log.debug("Creator {} nao curtiu outfit {}, ignorando unlike", creatorId, outfitId);
            return;
        }

        creatorNodeRepository.unlike(creatorId, outfitId);
        safeDecrement("likes:count:" + outfitId);
        log.info("Creator {} removeu curtida do outfit {}. Contador decrementado", creatorId, outfitId);
    }

    private static final Duration COUNTER_TTL = Duration.ofDays(30);

    private void safeIncrement(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, COUNTER_TTL);
        } catch (Exception e) {
            log.warn("Falha ao incrementar contador Redis '{}': {}", key, e.getMessage());
        }
    }

    private void safeDecrement(String key) {
        try {
            redisTemplate.opsForValue().decrement(key);
            redisTemplate.expire(key, COUNTER_TTL);
        } catch (Exception e) {
            log.warn("Falha ao decrementar contador Redis '{}': {}", key, e.getMessage());
        }
    }

}
