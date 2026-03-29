package com.thelook.ms_social.jobs;

import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FollowerCountJob {

    private final CreatorNodeRepository creatorNodeRepository;
    private final CreatorRepository creatorRepository;
    private final StringRedisTemplate redisTemplate;

    public FollowerCountJob(CreatorNodeRepository creatorNodeRepository,
                            CreatorRepository creatorRepository,
                            StringRedisTemplate redisTemplate) {
        this.creatorNodeRepository = creatorNodeRepository;
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(cron = "${schedule.followers.count.job}")
    public void recalculateFollowerCounts() {
        List<CreatorFollowerCount> counts = creatorNodeRepository.countFollowersPerCreator();
        for (CreatorFollowerCount count : counts) {
            creatorRepository.updateFollowerCount(UUID.fromString(count.creatorId()), count.total());
            redisTemplate.opsForValue().set(
                    "followers:count:" + count.creatorId(),
                    String.valueOf(count.total())
            );
        }
    }
}
