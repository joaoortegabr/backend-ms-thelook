package com.thelook.ms_social.jobs;

import com.thelook.ms_social.repositories.CreatorRepository;
import com.thelook.ms_social.repositories.CreatorRepository.CreatorFollowerProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class FollowerCacheWarmup {

    private static final Logger log = LoggerFactory.getLogger(FollowerCacheWarmup.class);
    private static final int BATCH_SIZE = 500;

    private final CreatorRepository creatorRepository;
    private final StringRedisTemplate redisTemplate;

    public FollowerCacheWarmup(CreatorRepository creatorRepository,
                               StringRedisTemplate redisTemplate) {
        this.creatorRepository = creatorRepository;
        this.redisTemplate = redisTemplate;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        log.info("Warmup de followers iniciado em background");
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        int total = 0;

        try {
            Page<CreatorFollowerProjection> page;
            do {
                page = creatorRepository.findActiveFollowerCounts(pageable);
                if (page.hasContent()) {
                    Map<String, String> entries = new HashMap<>(page.getNumberOfElements());
                    page.getContent().forEach(row ->
                        entries.put("followers:count:" + row.getId(), String.valueOf(row.getFollowersCount()))
                    );
                    redisTemplate.opsForValue().multiSet(entries);
                    total += entries.size();
                }
                pageable = pageable.next();
            } while (page.hasNext());

            log.info("Warmup de followers concluido: {} contadores restaurados", total);
        } catch (Exception e) {
            log.error("Erro no warmup de followers: {}", e.getMessage(), e);
        }
    }
}