package com.thelook.ms_social.jobs;

import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowerCountJobTest {

    @Mock private CreatorNodeRepository creatorNodeRepository;
    @Mock private CreatorRepository creatorRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private FollowerCountJob followerCountJob;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // =========================================================
    // recalculateFollowerCounts
    // =========================================================

    @Test
    void recalculate_validCounts_updatesPostgresAndRedisForEachCreator() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<CreatorFollowerCount> counts = List.of(
                new CreatorFollowerCount(id1.toString(), 42L),
                new CreatorFollowerCount(id2.toString(), 7L)
        );
        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(counts);

        followerCountJob.recalculateFollowerCounts();

        verify(creatorRepository).updateFollowerCount(id1, 42L);
        verify(creatorRepository).updateFollowerCount(id2, 7L);
        verify(valueOps).set("followers:count:" + id1, "42");
        verify(valueOps).set("followers:count:" + id2, "7");
    }

    @Test
    void recalculate_invalidUUID_skipsEntryAndContinuesWithRemainder() {
        UUID validId = UUID.randomUUID();
        List<CreatorFollowerCount> counts = List.of(
                new CreatorFollowerCount("not-a-valid-uuid", 10L),
                new CreatorFollowerCount(validId.toString(), 5L)
        );
        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(counts);

        followerCountJob.recalculateFollowerCounts();

        verify(creatorRepository, never()).updateFollowerCount(eq(null), anyLong());
        verify(creatorRepository).updateFollowerCount(validId, 5L);
        verify(valueOps).set("followers:count:" + validId, "5");
    }

    @Test
    void recalculate_emptyList_doesNotInteractWithRepositories() {
        when(creatorNodeRepository.countFollowersPerCreator()).thenReturn(List.of());

        followerCountJob.recalculateFollowerCounts();

        verify(creatorRepository, never()).updateFollowerCount(any(), anyLong());
        verify(valueOps, never()).set(anyString(), anyString());
    }

    @Test
    void recalculate_singleCreatorWithZeroFollowers_updatesWithZero() {
        UUID id = UUID.randomUUID();
        when(creatorNodeRepository.countFollowersPerCreator())
                .thenReturn(List.of(new CreatorFollowerCount(id.toString(), 0L)));

        followerCountJob.recalculateFollowerCounts();

        verify(creatorRepository).updateFollowerCount(id, 0L);
        verify(valueOps).set("followers:count:" + id, "0");
    }
}