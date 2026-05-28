package com.thelook.ms_social.jobs;

import com.thelook.ms_social.repositories.OutfitNodeRepository;
import com.thelook.ms_social.repositories.OutfitNodeRepository.OutfitLikeCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(2)
public class LikesCacheWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LikesCacheWarmup.class);

    private final OutfitNodeRepository outfitNodeRepository;
    private final StringRedisTemplate redisTemplate;

    public LikesCacheWarmup(OutfitNodeRepository outfitNodeRepository,
                             StringRedisTemplate redisTemplate) {
        this.outfitNodeRepository = outfitNodeRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<OutfitLikeCount> rows = outfitNodeRepository.findLikeCountsPerOutfit();

        if (rows.isEmpty()) {
            log.info("Warmup de likes: nenhum contador para restaurar");
            return;
        }

        Map<String, String> entries = new HashMap<>(rows.size());
        for (OutfitLikeCount row : rows) {
            entries.put("likes:count:" + row.getOutfitId(), String.valueOf(row.getLikeCount()));
        }

        redisTemplate.opsForValue().multiSet(entries);
        log.info("Warmup de likes concluido: {} contadores restaurados do Neo4j para o Redis", entries.size());
    }
}