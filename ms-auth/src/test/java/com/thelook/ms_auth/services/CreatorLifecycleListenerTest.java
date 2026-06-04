package com.thelook.ms_auth.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_auth.entities.User;
import com.thelook.ms_auth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorLifecycleListenerTest {

    @Mock UserRepository userRepository;

    @InjectMocks CreatorLifecycleListener listener;

    // =========================================================
    // PURGED
    // =========================================================

    @Test
    void handle_purgedEvent_userFound_deletesUser() {
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        listener.handle(new CreatorLifecycleEventDTO("PURGED", creatorId, userId));

        verify(userRepository).delete(user);
    }

    @Test
    void handle_purgedEvent_userNotFound_doesNotCallDelete() {
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        listener.handle(new CreatorLifecycleEventDTO("PURGED", creatorId, userId));

        verify(userRepository, never()).delete(any());
    }

    // =========================================================
    // Non-PURGED events
    // =========================================================

    @Test
    void handle_deactivatedEvent_returnsWithoutTouchingUserRepository() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.handle(new CreatorLifecycleEventDTO("DEACTIVATED", creatorId, userId));

        verifyNoInteractions(userRepository);
    }

    @Test
    void handle_reactivatedEvent_returnsWithoutTouchingUserRepository() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.handle(new CreatorLifecycleEventDTO("REACTIVATED", creatorId, userId));

        verifyNoInteractions(userRepository);
    }

    @Test
    void handle_unknownEventType_returnsWithoutTouchingUserRepository() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        listener.handle(new CreatorLifecycleEventDTO("UNKNOWN", creatorId, userId));

        verifyNoInteractions(userRepository);
    }

    // =========================================================
    // Exception propagation
    // =========================================================

    @Test
    void handle_purgedEvent_repositoryDeleteThrows_rethrowsException() {
        UUID userId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("DB error")).when(userRepository).delete(user);

        assertThatThrownBy(() -> listener.handle(new CreatorLifecycleEventDTO("PURGED", creatorId, userId)))
                .isInstanceOf(RuntimeException.class);
    }
}