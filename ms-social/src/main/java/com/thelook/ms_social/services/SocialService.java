package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SocialService {

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

        if (creatorNodeRepository.isFollowing(creatorId, targetId))
            return;

        creatorNodeRepository.follow(creatorId, targetId);
        redisTemplate.opsForValue().increment("followers:count:" + creatorId);
    }

    public void unfollow(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFollowing(creatorId, targetId))
            return;

        creatorNodeRepository.unfollow(creatorId, targetId);
        redisTemplate.opsForValue().decrement("followers:count:" + creatorId);
    }

}
