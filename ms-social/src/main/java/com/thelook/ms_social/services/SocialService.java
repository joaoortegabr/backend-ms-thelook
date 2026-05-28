package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    public void follow(UUID creatorId, UUID targetId) {

        if (creatorId.equals(targetId))
            throw new BusinessRuleException("Invalid request: you can't follow your own profile");

        if (creatorNodeRepository.isFollowing(creatorId, targetId)) {
            log.debug("Creator {} ja segue {}, ignorando", creatorId, targetId);
            return;
        }

        creatorNodeRepository.follow(creatorId, targetId);
        redisTemplate.opsForValue().increment("followers:count:" + targetId);
        log.info("Creator {} seguiu {}. Contador de seguidores de {} incrementado", creatorId, targetId, targetId);
    }

    public void unfollow(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFollowing(creatorId, targetId)) {
            log.debug("Creator {} nao segue {}, ignorando unfollow", creatorId, targetId);
            return;
        }

        creatorNodeRepository.unfollow(creatorId, targetId);
        redisTemplate.opsForValue().decrement("followers:count:" + targetId);
        log.info("Creator {} deixou de seguir {}. Contador decrementado", creatorId, targetId);
    }

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

    public void unfavorite(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFavorite(creatorId, targetId)) {
            log.debug("Creator {} nao favoritou {}, ignorando unfavorite", creatorId, targetId);
            return;
        }

        creatorNodeRepository.unfavorite(creatorId, targetId);
        log.info("Creator {} desfavoritou {}", creatorId, targetId);
    }

    public void likeOutfit(UUID creatorId, UUID outfitId) {

        if (creatorNodeRepository.isLiking(creatorId, outfitId)) {
            log.debug("Creator {} ja curtiu outfit {}, ignorando", creatorId, outfitId);
            return;
        }

        creatorNodeRepository.like(creatorId, outfitId);
        redisTemplate.opsForValue().increment("likes:count:" + outfitId);
        log.info("Creator {} curtiu outfit {}. Contador incrementado", creatorId, outfitId);
    }

    public void unlikeOutfit(UUID creatorId, UUID outfitId) {

        if (!creatorNodeRepository.isLiking(creatorId, outfitId)) {
            log.debug("Creator {} nao curtiu outfit {}, ignorando unlike", creatorId, outfitId);
            return;
        }

        creatorNodeRepository.unlike(creatorId, outfitId);
        redisTemplate.opsForValue().decrement("likes:count:" + outfitId);
        log.info("Creator {} removeu curtida do outfit {}. Contador decrementado", creatorId, outfitId);
    }

}
