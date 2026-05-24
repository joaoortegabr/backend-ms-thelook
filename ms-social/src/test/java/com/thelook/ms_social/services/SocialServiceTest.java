package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock private CreatorNodeRepository creatorNodeRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private SocialService socialService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // =========================================================
    // follow
    // =========================================================

    @Test
    void follow_notYetFollowing_createsRelationshipAndIncrementsCounter() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFollowing(creatorId, targetId)).thenReturn(false);

        socialService.follow(creatorId, targetId);

        verify(creatorNodeRepository).follow(creatorId, targetId);
        verify(valueOps).increment("followers:count:" + targetId);
    }

    @Test
    void follow_alreadyFollowing_isIdempotentAndDoesNotDuplicate() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFollowing(creatorId, targetId)).thenReturn(true);

        socialService.follow(creatorId, targetId);

        verify(creatorNodeRepository, never()).follow(any(), any());
        verify(valueOps, never()).increment(anyString());
    }

    @Test
    void follow_sameCreatorAndTargetId_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> socialService.follow(id, id))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(creatorNodeRepository);
        verifyNoInteractions(valueOps);
    }

    // =========================================================
    // unfollow
    // =========================================================

    @Test
    void unfollow_isFollowing_removesRelationshipAndDecrementsCounter() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFollowing(creatorId, targetId)).thenReturn(true);

        socialService.unfollow(creatorId, targetId);

        verify(creatorNodeRepository).unfollow(creatorId, targetId);
        verify(valueOps).decrement("followers:count:" + targetId);
    }

    @Test
    void unfollow_notFollowing_isIdempotentAndDoesNotDecrement() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFollowing(creatorId, targetId)).thenReturn(false);

        socialService.unfollow(creatorId, targetId);

        verify(creatorNodeRepository, never()).unfollow(any(), any());
        verify(valueOps, never()).decrement(anyString());
    }
}