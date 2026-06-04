package com.thelook.ms_social.services;

import com.thelook.ms_social.entities.OutboxMessage;
import com.thelook.ms_social.repositories.OutboxRepository;
import com.thelook.ms_social.services.events.OutboxSavedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private static final long OUTBOX_RELAY_DELAY_MS = 200;
    private static final int MAX_RETRIES = 5;

    private final AtomicBoolean dirty = new AtomicBoolean(false);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(OutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxSaved(OutboxSavedEvent event) {
        dirty.set(true);
    }

    @Scheduled(fixedDelay = OUTBOX_RELAY_DELAY_MS)
    @Transactional(transactionManager = "transactionManager")
    public void publishMessages() {
        if (!dirty.compareAndSet(true, false)) return;

        Pageable page = PageRequest.of(0, 100);
        List<OutboxMessage> messages = outboxRepository.findByProcessedFalseAndRetryCountLessThan(MAX_RETRIES, page);
        List<UUID> processedIds = new ArrayList<>();

        for (OutboxMessage message : messages) {
            String routingKey = switch (message.getType()) {
                case "CREATOR_DEACTIVATED", "CREATOR_REACTIVATED", "CREATOR_PURGED" -> "creator.lifecycle";
                default -> {
                    log.warn("Tipo desconhecido no Outbox: {} (id={})", message.getType(), message.getId());
                    yield null;
                }
            };

            if (routingKey == null) {
                processedIds.add(message.getId());
                continue;
            }

            try {
                final String rk = routingKey;
                rabbitTemplate.invoke(ops -> {
                    ops.convertAndSend("ex.thelook.creator", rk, message.getPayload());
                    ops.waitForConfirmsOrDie(5000);
                    return null;
                });
                processedIds.add(message.getId());
            } catch (Exception e) {
                log.error("Falha ao enviar mensagem do Outbox {} (tentativa {}): {}",
                        message.getId(), message.getRetryCount() + 1, e.getMessage(), e);
                outboxRepository.incrementRetryCount(message.getId());
            }
        }

        if (!processedIds.isEmpty()) {
            outboxRepository.markAsProcessed(processedIds);
        }
    }

    @Scheduled(cron = "0 0 5 * * *")
    @Transactional(transactionManager = "transactionManager")
    public void purgeProcessedMessages() {
        outboxRepository.deleteProcessedBefore(LocalDateTime.now().minusDays(7));
        log.info("Outbox: mensagens processadas com mais de 7 dias removidas");
    }
}