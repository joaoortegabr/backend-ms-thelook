package com.thelook.ms_social.jobs;

import com.thelook.ms_social.repositories.CreatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class FollowerCacheWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FollowerCacheWarmup.class);

    private final CreatorRepository creatorRepository;
    private final StringRedisTemplate redisTemplate;

    public FollowerCacheWarmup(CreatorRepository creatorRepository,
                               StringRedisTemplate redisTemplate) {
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Object[]> rows = creatorRepository.findActiveFollowerCounts();

        if (rows.isEmpty()) {
            log.info("Warmup de followers: nenhum contador para restaurar");
            return;
        }

        Map<String, String> entries = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            UUID creatorId = (UUID) row[0];
            long count     = (long)  row[1];
            entries.put("followers:count:" + creatorId, String.valueOf(count));
        }

        redisTemplate.opsForValue().multiSet(entries);
        log.info("Warmup de followers concluido: {} contadores restaurados do PostgreSQL para o Redis", entries.size());
    }
}