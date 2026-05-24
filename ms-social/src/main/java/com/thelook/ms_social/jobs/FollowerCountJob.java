package com.thelook.ms_social.jobs;

import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class FollowerCountJob {

    private static final Logger log = LoggerFactory.getLogger(FollowerCountJob.class);

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
    @Transactional(transactionManager = "transactionManager")
    public void recalculateFollowerCounts() {
        log.info("Iniciando recalculo de contagem de seguidores");
        List<CreatorFollowerCount> counts = creatorNodeRepository.countFollowersPerCreator();
        int processed = 0;

        for (CreatorFollowerCount count : counts) {
            try {
                UUID creatorId = UUID.fromString(count.creatorId());
                creatorRepository.updateFollowerCount(creatorId, count.total());
                redisTemplate.opsForValue().set(
                        "followers:count:" + count.creatorId(),
                        String.valueOf(count.total())
                );
                processed++;
            } catch (IllegalArgumentException e) {
                log.error("creatorId invalido no Neo4j: '{}'. Ignorando.", count.creatorId(), e);
            }
        }

        log.info("Recalculo de seguidores concluido: {}/{} registros processados", processed, counts.size());
    }
}
