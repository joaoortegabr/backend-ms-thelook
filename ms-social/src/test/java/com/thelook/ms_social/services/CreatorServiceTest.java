package com.thelook.ms_social.services;

import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.models.dtos.CreatorRequest;
import com.thelook.ms_social.models.dtos.CreatorUpdateRequest;
import com.thelook.ms_social.repositories.CreatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorServiceTest {

    @Mock private CreatorNodeService creatorNodeService;
    @Mock private CreatorRepository creatorRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private CreatorService creatorService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    private Creator creator(UUID id, UUID userId) {
        Creator c = new Creator();
        c.setId(id);
        c.setUserId(userId);
        c.setName("Alice");
        c.setInstagram("alice_ig");
        c.setIsActive(true);
        return c;
    }

    private CreatorRequest request() {
        return new CreatorRequest("Alice", null, "Bio", "alice_ig",
                LocalDate.of(1995, 1, 1), "São Paulo", "SP");
    }

    // =========================================================
    // findById
    // =========================================================

    @Test
    void findById_creatorExists_returnsCreator() {
        UUID id = UUID.randomUUID();
        Creator expected = creator(id, UUID.randomUUID());
        when(creatorRepository.findIdByCreatorId(id)).thenReturn(Optional.of(expected));

        Creator result = creatorService.findById(id);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findById_creatorNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(creatorRepository.findIdByCreatorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creatorService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================
    // create
    // =========================================================

    @Test
    void create_success_savesToPostgresNeo4jAndRedis() {
        UUID userId = UUID.randomUUID();

        doAnswer(inv -> {
            Creator c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        }).when(creatorRepository).save(any(Creator.class));

        Creator result = creatorService.create(userId, request());

        ArgumentCaptor<Creator> captor = ArgumentCaptor.forClass(Creator.class);
        verify(creatorRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
        assertThat(captor.getValue().getIsActive()).isTrue();

        verify(creatorNodeService).save(any(Creator.class));
        verify(valueOps).set(eq("user:profile:" + userId), anyString(), eq(Duration.ofDays(90)));
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    void create_repositoryThrows_throwsBusinessRuleException() {
        UUID userId = UUID.randomUUID();
        when(creatorRepository.save(any())).thenThrow(new RuntimeException("constraint violation"));

        assertThatThrownBy(() -> creatorService.create(userId, request()))
                .isInstanceOf(BusinessRuleException.class);

        verify(creatorNodeService, never()).save(any());
    }

    // =========================================================
    // update
    // =========================================================

    @Test
    void update_allFieldsProvided_updatesAllFields() {
        UUID creatorId = UUID.randomUUID();
        Creator existing = creator(creatorId, UUID.randomUUID());
        existing.setAvatarUrl("old-avatar");
        existing.setBio("old bio");
        existing.setInstagram("old_ig");
        existing.setCity("Old City");
        existing.setUf("RJ");

        when(creatorRepository.findIdByCreatorId(creatorId)).thenReturn(Optional.of(existing));
        when(creatorRepository.save(any())).thenReturn(existing);

        CreatorUpdateRequest req = new CreatorUpdateRequest("new-avatar", "new bio", "new_ig", "New City", "SP");
        Creator result = creatorService.update(creatorId, req);

        assertThat(result.getAvatarUrl()).isEqualTo("new-avatar");
        assertThat(result.getBio()).isEqualTo("new bio");
        assertThat(result.getInstagram()).isEqualTo("new_ig");
        assertThat(result.getCity()).isEqualTo("New City");
        assertThat(result.getUf()).isEqualTo("SP");
        verify(creatorRepository).save(existing);
    }

    @Test
    void update_nullFields_doesNotOverwriteExistingValues() {
        UUID creatorId = UUID.randomUUID();
        Creator existing = creator(creatorId, UUID.randomUUID());
        existing.setAvatarUrl("keep-avatar");
        existing.setBio("keep bio");

        when(creatorRepository.findIdByCreatorId(creatorId)).thenReturn(Optional.of(existing));
        when(creatorRepository.save(any())).thenReturn(existing);

        CreatorUpdateRequest req = new CreatorUpdateRequest(null, null, "new_ig", null, null);
        Creator result = creatorService.update(creatorId, req);

        assertThat(result.getAvatarUrl()).isEqualTo("keep-avatar");
        assertThat(result.getBio()).isEqualTo("keep bio");
        assertThat(result.getInstagram()).isEqualTo("new_ig");
    }

    @Test
    void update_creatorNotFound_throwsResourceNotFoundException() {
        UUID creatorId = UUID.randomUUID();
        when(creatorRepository.findIdByCreatorId(creatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creatorService.update(creatorId, new CreatorUpdateRequest(null, null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(creatorRepository, never()).save(any());
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_success_deletesFromAllStoresAndReturnsMessage() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Creator existing = creator(creatorId, userId);

        when(creatorRepository.findIdByCreatorId(creatorId)).thenReturn(Optional.of(existing));

        String result = creatorService.delete(creatorId);

        verify(creatorRepository).deleteById(creatorId);
        verify(creatorNodeService).delete(creatorId);
        verify(redisTemplate).delete("user:profile:" + userId);
        assertThat(result).isEqualTo("Registro removido com sucesso.");
    }

    @Test
    void delete_creatorNotFound_throwsResourceNotFoundException() {
        UUID creatorId = UUID.randomUUID();
        when(creatorRepository.findIdByCreatorId(creatorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creatorService.delete(creatorId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(creatorRepository, never()).deleteById(any());
    }
}