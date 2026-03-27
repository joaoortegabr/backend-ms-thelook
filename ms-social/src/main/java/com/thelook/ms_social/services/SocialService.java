package com.thelook.ms_social.services;

import com.thelook.security.UserContext;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.IncompleteProfileException;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SocialService {

    private final CreatorNodeRepository creatorNodeRepository;

    public SocialService(CreatorNodeRepository creatorNodeRepository) {
        this.creatorNodeRepository = creatorNodeRepository;
    }

    public void follow(UUID creatorId, UUID targetId) {

        if (creatorId.equals(targetId))
            throw new BusinessRuleException("Invalid request: you can't follow your own profile");

        if (creatorNodeRepository.isFollowing(creatorId, targetId))
            return;

        creatorNodeRepository.follow(creatorId, targetId);
    }

    public void unfollow(UUID creatorId, UUID targetId) {

        if (!creatorNodeRepository.isFollowing(creatorId, targetId))
            return;

        creatorNodeRepository.unfollow(creatorId, targetId);
    }

}
