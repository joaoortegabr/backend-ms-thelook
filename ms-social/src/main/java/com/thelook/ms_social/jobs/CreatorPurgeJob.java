package com.thelook.ms_social.jobs;

import com.thelook.ms_social.entities.Creator;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.repositories.CreatorRepository;
import com.thelook.ms_social.services.CreatorEventPublisher;
import com.thelook.ms_social.services.CreatorNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CreatorPurgeJob {

    private static final Logger log = LoggerFactory.getLogger(CreatorPurgeJob.class);
    private static final int RETENTION_DAYS = 90;

    private final CreatorRepository creatorRepository;
    private final CreatorNodeService creatorNodeService;
    private final StringRedisTemplate redisTemplate;
    private final CreatorEventPublisher creatorEventPublisher;

    public CreatorPurgeJob(CreatorRepository creatorRepository,
                           CreatorNodeService creatorNodeService,
                           StringRedisTemplate redisTemplate,
                           CreatorEventPublisher creatorEventPublisher) {
        this.creatorRepository = creatorRepository;
        this.creatorNodeService = creatorNodeService;
        this.redisTemplate = redisTemplate;
        this.creatorEventPublisher = creatorEventPublisher;
    }

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional(transactionManager = "transactionManager")
    public void purgeExpiredCreators() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<Creator> expired = creatorRepository.findExpiredDeletedCreators(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Iniciando purga de {} creators expirados", expired.size());

        for (Creator creator : expired) {
            try {
                creatorRepository.deleteById(creator.getId());
                try {
                    creatorNodeService.delete(creator.getId());
                } catch (ResourceNotFoundException ignored) {
                    // nó já removido no soft delete — comportamento esperado
                }
                redisTemplate.delete("followers:count:" + creator.getId());
                creatorEventPublisher.publishPurged(creator.getId(), creator.getUserId());
                log.info("Creator {} purgado permanentemente", creator.getId());
            } catch (Exception e) {
                log.error("Falha ao purgar creator {}: {}", creator.getId(), e.getMessage(), e);
            }
        }

        log.info("Purga concluida: {}/{} creators removidos", expired.size(), expired.size());
    }
}