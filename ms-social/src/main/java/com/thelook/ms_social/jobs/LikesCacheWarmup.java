package com.thelook.ms_social.jobs;

import com.thelook.ms_social.repositories.OutfitNodeRepository;
import com.thelook.ms_social.repositories.OutfitNodeRepository.OutfitLikeCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class LikesCacheWarmup {

    private static final Logger log = LoggerFactory.getLogger(LikesCacheWarmup.class);
    private static final Duration REDIS_TTL = Duration.ofDays(30);

    private final OutfitNodeRepository outfitNodeRepository;
    private final StringRedisTemplate redisTemplate;

    public LikesCacheWarmup(OutfitNodeRepository outfitNodeRepository,
                             StringRedisTemplate redisTemplate) {
        this.outfitNodeRepository = outfitNodeRepository;
        this.redisTemplate = redisTemplate;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        log.info("Warmup de likes iniciado em background");
        try {
            List<OutfitLikeCount> rows = outfitNodeRepository.findLikeCountsPerOutfit();

            if (rows.isEmpty()) {
                log.info("Warmup de likes: nenhum contador para restaurar");
                return;
            }

            long ttlSeconds = REDIS_TTL.getSeconds();
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (OutfitLikeCount row : rows) {
                    byte[] key = ("likes:count:" + row.getOutfitId()).getBytes(StandardCharsets.UTF_8);
                    byte[] val = String.valueOf(row.getLikeCount()).getBytes(StandardCharsets.UTF_8);
                    connection.stringCommands().setEx(key, ttlSeconds, val);
                }
                return null;
            });
            log.info("Warmup de likes concluido: {} contadores restaurados", rows.size());
        } catch (Exception e) {
            log.error("Erro no warmup de likes: {}", e.getMessage(), e);
        }
    }
}