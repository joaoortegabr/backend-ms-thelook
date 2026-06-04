package com.thelook.ms_social.jobs;

import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowerCountJobTest {

    @Mock private CreatorNodeRepository creatorNodeRepository;
    @Mock private CreatorRepository creatorRepository;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks private FollowerCountJob followerCountJob;

    // =========================================================
    // recalculateFollowerCounts
    // =========================================================

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_validCounts_updatesPostgresAndSendsRedisPipeline() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Creator c1 = new Creator(); c1.setId(id1);
        Creator c2 = new Creator(); c2.setId(id2);

        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(List.of(
                new CreatorFollowerCount(id1.toString(), 42L),
                new CreatorFollowerCount(id2.toString(), 7L)));
        when(creatorRepository.findAllById(any())).thenReturn(List.of(c1, c2));
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        followerCountJob.recalculateFollowerCounts();

        ArgumentCaptor<List<Creator>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(creatorRepository).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue())
                .extracting(Creator::getFollowersCount)
                .containsExactlyInAnyOrder(42L, 7L);

        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_invalidUUID_skipsEntryAndContinuesWithRemainder() {
        UUID validId = UUID.randomUUID();
        Creator c = new Creator(); c.setId(validId);

        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(List.of(
                new CreatorFollowerCount("not-a-valid-uuid", 10L),
                new CreatorFollowerCount(validId.toString(), 5L)));
        when(creatorRepository.findAllById(any())).thenReturn(List.of(c));
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        followerCountJob.recalculateFollowerCounts();

        ArgumentCaptor<List<Creator>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(creatorRepository).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue()).hasSize(1);
        assertThat(saveCaptor.getValue().get(0).getFollowersCount()).isEqualTo(5L);

        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    void recalculate_emptyList_doesNotInteractWithRepositories() {
        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(List.of());

        followerCountJob.recalculateFollowerCounts();

        verify(creatorRepository, never()).findAllById(any());
        verify(creatorRepository, never()).saveAll(any());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void recalculate_singleCreatorWithZeroFollowers_updatesWithZero() {
        UUID id = UUID.randomUUID();
        Creator c = new Creator(); c.setId(id);

        when(creatorNodeRepository.countFollowersPerCreator())
                .thenReturn(List.of(new CreatorFollowerCount(id.toString(), 0L)));
        when(creatorRepository.findAllById(any())).thenReturn(List.of(c));
        when(redisTemplate.executePipelined(any(RedisCallback.class))).thenReturn(List.of());

        followerCountJob.recalculateFollowerCounts();

        ArgumentCaptor<List<Creator>> saveCaptor = ArgumentCaptor.forClass(List.class);
        verify(creatorRepository).saveAll(saveCaptor.capture());
        assertThat(saveCaptor.getValue().get(0).getFollowersCount()).isEqualTo(0L);

        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }
}
