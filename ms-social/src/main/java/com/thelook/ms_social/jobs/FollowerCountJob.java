package com.thelook.ms_social.jobs;

import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorFollowerCount;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class FollowerCountJob {

    private static final Logger log = LoggerFactory.getLogger(FollowerCountJob.class);
    private static final Duration REDIS_TTL = Duration.ofDays(2);

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

        Map<UUID, Long> countMap = new HashMap<>();
        for (CreatorFollowerCount count : counts) {
            try {
                countMap.put(UUID.fromString(count.creatorId()), count.total());
            } catch (IllegalArgumentException e) {
                log.error("creatorId invalido no Neo4j: '{}'. Ignorando.", count.creatorId(), e);
            }
        }

        if (countMap.isEmpty()) {
            log.info("Nenhum dado de seguidores para processar");
            return;
        }

        List<Creator> creators = creatorRepository.findAllById(countMap.keySet());
        creators.forEach(c -> c.setFollowersCount(countMap.get(c.getId())));
        creatorRepository.saveAll(creators);

        Map<String, String> redisMap = countMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> "followers:count:" + e.getKey(),
                        e -> String.valueOf(e.getValue())
                ));

        long ttlSeconds = REDIS_TTL.getSeconds();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            redisMap.forEach((key, value) -> {
                byte[] rawKey = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] rawVal = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                connection.stringCommands().setEx(rawKey, ttlSeconds, rawVal);
            });
            return null;
        });

        log.info("Recalculo de seguidores concluido: {}/{} registros processados", creators.size(), counts.size());
    }
}