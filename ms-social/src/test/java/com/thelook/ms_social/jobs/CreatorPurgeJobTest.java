package com.thelook.ms_social.jobs;

import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.repositories.CreatorRepository;
import com.thelook.ms_social.services.CreatorEventPublisher;
import com.thelook.ms_social.services.CreatorNodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

        verify(creatorRepository, never()).deleteAllByIdInBatch(any());
        verifyNoInteractions(creatorNodeService);
        verifyNoInteractions(creatorEventPublisher);
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void purgeExpiredCreators_umCreator_executaBatchDeleteEPublicaEvento() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Creator creator = creator(creatorId, userId);
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator));

        creatorPurgeJob.purgeExpiredCreators();

        // 1 batch delete em vez de N deleteById individuais
        ArgumentCaptor<Iterable<UUID>> batchCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(creatorRepository).deleteAllByIdInBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).containsExactly(creatorId);

        verify(creatorNodeService).deleteAll(List.of(creatorId));

        // Batch Redis delete
        ArgumentCaptor<Collection<String>> redisCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(redisTemplate).delete(redisCaptor.capture());
        assertThat(redisCaptor.getValue()).containsExactly("followers:count:" + creatorId);

        verify(creatorEventPublisher).publishPurged(creatorId, userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void purgeExpiredCreators_neo4jDeleteAllThrows_continuaComRedisEEventos() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Creator creator = creator(creatorId, userId);
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator));
        doThrow(new RuntimeException("Neo4j unavailable")).when(creatorNodeService).deleteAll(any());

        creatorPurgeJob.purgeExpiredCreators();

        verify(creatorRepository).deleteAllByIdInBatch(any());
        verify(redisTemplate).delete(any(Collection.class));
        verify(creatorEventPublisher).publishPurged(creatorId, userId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void purgeExpiredCreators_multiplosCreators_deletaEmBatch() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Creator creator1 = creator(id1, UUID.randomUUID());
        Creator creator2 = creator(id2, UUID.randomUUID());
        when(creatorRepository.findExpiredDeletedCreators(any())).thenReturn(List.of(creator1, creator2));

        creatorPurgeJob.purgeExpiredCreators();

        // Apenas 1 chamada batch — não N chamadas individuais
        verify(creatorRepository, times(1)).deleteAllByIdInBatch(any());

        ArgumentCaptor<List<UUID>> nodeCaptor = ArgumentCaptor.forClass(List.class);
        verify(creatorNodeService).deleteAll(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue()).containsExactlyInAnyOrder(id1, id2);

        verify(creatorEventPublisher).publishPurged(eq(id1), any());
        verify(creatorEventPublisher).publishPurged(eq(id2), any());
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
