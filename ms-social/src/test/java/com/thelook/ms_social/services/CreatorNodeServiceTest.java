package com.thelook.ms_social.services;

import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_social.entities.Creator;
import com.thelook.ms_social.entities.CreatorNode;
import com.thelook.ms_social.repositories.CreatorNodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorNodeServiceTest {

    @Mock private CreatorNodeRepository creatorNodeRepository;

    @InjectMocks private CreatorNodeService creatorNodeService;

    private Creator creator(UUID id, String name) {
        Creator c = new Creator();
        c.setId(id);
        c.setName(name);
        return c;
    }

    // =========================================================
    // save
    // =========================================================

    @Test
    void save_createsNodeWithCreatorIdAndName() {
        UUID id = UUID.randomUUID();
        Creator creator = creator(id, "Alice");

        creatorNodeService.save(creator);

        ArgumentCaptor<CreatorNode> captor = ArgumentCaptor.forClass(CreatorNode.class);
        verify(creatorNodeRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatorId()).isEqualTo(id);
        assertThat(captor.getValue().getName()).isEqualTo("Alice");
    }

    // =========================================================
    // delete
    // =========================================================

    @Test
    void delete_nodeExists_callsDeepDelete() {
        UUID creatorId = UUID.randomUUID();
        when(creatorNodeRepository.existsById(creatorId)).thenReturn(true);

        creatorNodeService.delete(creatorId);

        verify(creatorNodeRepository).deepDeleteCreator(creatorId);
    }

    @Test
    void delete_nodeNotFound_throwsResourceNotFoundException() {
        UUID creatorId = UUID.randomUUID();
        when(creatorNodeRepository.existsById(creatorId)).thenReturn(false);

        assertThatThrownBy(() -> creatorNodeService.delete(creatorId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(creatorNodeRepository, never()).deepDeleteCreator(any());
    }
}