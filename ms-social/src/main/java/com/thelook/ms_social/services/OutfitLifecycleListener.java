package com.thelook.ms_social.services;

import com.thelook.dtos.OutfitDeletedDTO;
import com.thelook.ms_social.repositories.OutfitNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutfitLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(OutfitLifecycleListener.class);

    private final OutfitNodeRepository outfitNodeRepository;
    private final StringRedisTemplate redisTemplate;

    public OutfitLifecycleListener(OutfitNodeRepository outfitNodeRepository,
                                   StringRedisTemplate redisTemplate) {
        this.outfitNodeRepository = outfitNodeRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional(transactionManager = "neo4jTransactionManager")
    @RabbitListener(queues = "#{T(com.thelook.ms_social.config.RabbitMQConfig).QUEUE_OUTFIT_DELETED}")
    public void handleOutfitDeleted(OutfitDeletedDTO dto) {
        log.info("Removendo likes do outfit deletado: outfitId={}", dto.outfitId());
        try {
            outfitNodeRepository.deleteWithRelationships(dto.outfitId());
            redisTemplate.delete("likes:count:" + dto.outfitId());
            log.info("Outfit {} removido do grafo e contador de likes deletado", dto.outfitId());
        } catch (Exception e) {
            log.error("Falha ao processar deleção do outfit {}: {}. Mensagem será enviada para a DLQ.",
                    dto.outfitId(), e.getMessage(), e);
            throw e;
        }
    }
}
