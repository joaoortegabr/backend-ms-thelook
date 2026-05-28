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

    // =========================================================
    // favorite
    // =========================================================

    @Test
    void favorite_notYetFavorited_createsRelationship() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFavorite(creatorId, targetId)).thenReturn(false);

        socialService.favorite(creatorId, targetId);

        verify(creatorNodeRepository).favorite(creatorId, targetId);
    }

    @Test
    void favorite_alreadyFavorited_isIdempotentAndDoesNotDuplicate() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFavorite(creatorId, targetId)).thenReturn(true);

        socialService.favorite(creatorId, targetId);

        verify(creatorNodeRepository, never()).favorite(any(), any());
    }

    @Test
    void favorite_sameCreatorAndTargetId_throwsBusinessRuleException() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> socialService.favorite(id, id))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(creatorNodeRepository);
    }

    // =========================================================
    // unfavorite
    // =========================================================

    @Test
    void unfavorite_isFavorited_removesRelationship() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFavorite(creatorId, targetId)).thenReturn(true);

        socialService.unfavorite(creatorId, targetId);

        verify(creatorNodeRepository).unfavorite(creatorId, targetId);
    }

    @Test
    void unfavorite_notFavorited_isIdempotent() {
        UUID creatorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(creatorNodeRepository.isFavorite(creatorId, targetId)).thenReturn(false);

        socialService.unfavorite(creatorId, targetId);

        verify(creatorNodeRepository, never()).unfavorite(any(), any());
    }

    // =========================================================
    // likeOutfit
    // =========================================================

    @Test
    void likeOutfit_notYetLiked_createsRelationshipAndIncrementsCounter() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        when(creatorNodeRepository.isLiking(creatorId, outfitId)).thenReturn(false);

        socialService.likeOutfit(creatorId, outfitId);

        verify(creatorNodeRepository).like(creatorId, outfitId);
        verify(valueOps).increment("likes:count:" + outfitId);
    }

    @Test
    void likeOutfit_alreadyLiked_isIdempotentAndDoesNotDuplicate() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        when(creatorNodeRepository.isLiking(creatorId, outfitId)).thenReturn(true);

        socialService.likeOutfit(creatorId, outfitId);

        verify(creatorNodeRepository, never()).like(any(), any());
        verify(valueOps, never()).increment(anyString());
    }

    // =========================================================
    // unlikeOutfit
    // =========================================================

    @Test
    void unlikeOutfit_isLiked_removesRelationshipAndDecrementsCounter() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        when(creatorNodeRepository.isLiking(creatorId, outfitId)).thenReturn(true);

        socialService.unlikeOutfit(creatorId, outfitId);

        verify(creatorNodeRepository).unlike(creatorId, outfitId);
        verify(valueOps).decrement("likes:count:" + outfitId);
    }

    @Test
    void unlikeOutfit_notLiked_isIdempotentAndDoesNotDecrement() {
        UUID creatorId = UUID.randomUUID();
        UUID outfitId = UUID.randomUUID();
        when(creatorNodeRepository.isLiking(creatorId, outfitId)).thenReturn(false);

        socialService.unlikeOutfit(creatorId, outfitId);

        verify(creatorNodeRepository, never()).unlike(any(), any());
        verify(valueOps, never()).decrement(anyString());
    }
}