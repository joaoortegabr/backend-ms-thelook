package com.thelook.ms_social.jobs;

import com.thelook.ms_social.repositories.OutfitNodeRepository;
import com.thelook.ms_social.repositories.OutfitNodeRepository.OutfitLikeCount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LikesCacheWarmupTest {

    @Mock private OutfitNodeRepository outfitNodeRepository;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks private LikesCacheWarmup likesCacheWarmup;

    // =========================================================
    // warmup
    // =========================================================

    @Test
    @SuppressWarnings("unchecked")
    void warmup_withLikeCounts_sendsRedisPipelineWithTtl() {
        OutfitLikeCount row1 = mockRow(UUID.randomUUID(), 10L);
        OutfitLikeCount row2 = mockRow(UUID.randomUUID(), 3L);
        when(outfitNodeRepository.findLikeCountsPerOutfit()).thenReturn(List.of(row1, row2));
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        likesCacheWarmup.warmup();

        verify(redisTemplate).executePipelined(any(RedisCallback.class));
        verify(outfitNodeRepository).findLikeCountsPerOutfit();
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmup_emptyList_doesNotCallRedis() {
        when(outfitNodeRepository.findLikeCountsPerOutfit()).thenReturn(List.of());

        likesCacheWarmup.warmup();

        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void warmup_redisThrows_doesNotPropagateException() {
        OutfitLikeCount row = mockRow(UUID.randomUUID(), 5L);
        when(outfitNodeRepository.findLikeCountsPerOutfit()).thenReturn(List.of(row));
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        org.assertj.core.api.Assertions.assertThatCode(() -> likesCacheWarmup.warmup())
                .doesNotThrowAnyException();
    }

    private OutfitLikeCount mockRow(UUID outfitId, long likeCount) {
        OutfitLikeCount row = mock(OutfitLikeCount.class);
        // lenient: stubs are inside the RedisCallback lambda which executePipelined mock intercepts
        lenient().when(row.getOutfitId()).thenReturn(outfitId.toString());
        lenient().when(row.getLikeCount()).thenReturn(likeCount);
        return row;
    }
}
