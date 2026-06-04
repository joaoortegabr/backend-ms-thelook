package com.thelook.ms_social.services;

import com.thelook.ms_social.services.events.CreatorPersistEvent;
import com.thelook.ms_social.services.events.CreatorRemoveFromGraphEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CreatorGraphEventListener {

    private static final Logger log = LoggerFactory.getLogger(CreatorGraphEventListener.class);

    private final CreatorNodeService creatorNodeService;

    public CreatorGraphEventListener(CreatorNodeService creatorNodeService) {
        this.creatorNodeService = creatorNodeService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "neo4jTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void onCreatorPersisted(CreatorPersistEvent event) {
        try {
            creatorNodeService.save(event.creator());
            log.info("Creator {} salvo no grafo Neo4j apos commit JPA", event.creator().getId());
        } catch (Exception e) {
            log.error("Falha ao salvar creator {} no grafo Neo4j: {}", event.creator().getId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(transactionManager = "neo4jTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void onCreatorRemovedFromGraph(CreatorRemoveFromGraphEvent event) {
        try {
            creatorNodeService.delete(event.creatorId());
            log.info("Creator {} removido do grafo Neo4j apos commit JPA", event.creatorId());
        } catch (Exception e) {
            log.error("Falha ao remover creator {} do grafo Neo4j: {}", event.creatorId(), e.getMessage(), e);
        }
    }
}