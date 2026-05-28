package com.thelook.ms_auth.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_auth.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class CreatorLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(CreatorLifecycleListener.class);

    private final UserRepository userRepository;

    public CreatorLifecycleListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "q.creator.lifecycle.auth")
    public void handle(CreatorLifecycleEventDTO event) {
        if (!"PURGED".equals(event.type())) {
            return;
        }
        try {
            userRepository.findById(event.userId()).ifPresentOrElse(
                    user -> {
                        userRepository.delete(user);
                        log.info("User {} removido apos purga do creator {}", event.userId(), event.creatorId());
                    },
                    () -> log.warn("User {} nao encontrado para purga do creator {}", event.userId(), event.creatorId())
            );
        } catch (Exception e) {
            log.error("Falha ao remover user {} na purga do creator {}: {}", event.userId(), event.creatorId(), e.getMessage(), e);
            throw e;
        }
    }
}