package com.thelook.ms_social.jobs;

import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.repositories.CreatorRepository;
import com.thelook.ms_social.services.CreatorEventPublisher;
import com.thelook.ms_social.services.CreatorNodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatorPurgeJobTest {

    @Mock CreatorRepository creatorRepository;
    @Mock CreatorNodeService creatorNodeService;
    @Mock StringRedisTemplate redisTemplate;
    @Mock CreatorEventPublisher creatorEventPublisher;

    CreatorPurgeJob creatorPurgeJob;

    @BeforeEach
    void setUp() {
        creatorPurgeJob = new CreatorPurgeJob(creatorRepository, creatorNodeService, redisTemplate,
                creatorEventPublisher);
    }

    // ── purgeExpiredCreators ───────────────────────────────────────────────────

    @Test
    void purgeExpiredCreators_listaVazia_naoFazNada() {
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of());

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository, never()).deleteById(any());
        verifyNoInteractions(creatorNodeService);
        verifyNoInteractions(creatorEventPublisher);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void purgeExpiredCreators_umCreator_executaTodosPassosDaPurga() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Creator creator = creator(creatorId, userId);
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator));

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository).deleteById(creatorId);
        verify(creatorNodeService).delete(creatorId);
        verify(redisTemplate).delete("followers:count:" + creatorId);
        verify(creatorEventPublisher).publishPurged(creatorId, userId);
    }

    @Test
    void purgeExpiredCreators_neo4jNodeJaRemovido_continuaPurgaNormalmente() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Creator creator = creator(creatorId, userId);
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator));
        doThrow(new ResourceNotFoundException("Creator not found")).when(creatorNodeService).delete(creatorId);

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository).deleteById(creatorId);
        verify(redisTemplate).delete("followers:count:" + creatorId);
        verify(creatorEventPublisher).publishPurged(creatorId, userId);
    }

    @Test
    void purgeExpiredCreators_deleteThrowsException_continuaComProximoCreator() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Creator creator1 = creator(id1, UUID.randomUUID());
        Creator creator2 = creator(id2, userId2);
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator1, creator2));
        doThrow(new RuntimeException("DB failure")).when(creatorRepository).deleteById(id1);

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository).deleteById(id2);
        verify(creatorEventPublisher).publishPurged(id2, userId2);
    }

    @Test
    void purgeExpiredCreators_multiplosCreators_purgaTodos() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Creator creator1 = creator(id1, UUID.randomUUID());
        Creator creator2 = creator(id2, UUID.randomUUID());
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator1, creator2));

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository).deleteById(id1);
        verify(creatorRepository).deleteById(id2);
        verify(creatorNodeService).delete(id1);
        verify(creatorNodeService).delete(id2);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Creator creator(UUID id, UUID userId) {
        Creator c = new Creator();
        c.setId(id);
        c.setUserId(userId);
        c.setIsActive(false);
        return c;
    }
}
