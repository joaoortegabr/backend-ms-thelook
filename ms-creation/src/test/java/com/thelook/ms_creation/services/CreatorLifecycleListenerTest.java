package com.thelook.ms_creation.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorLifecycleListenerTest {

    @Mock OutfitRepository outfitRepository;
    @Mock StorageService storageService;

    @InjectMocks CreatorLifecycleListener listener;

    private CreatorLifecycleEventDTO event(String type, UUID creatorId) {
        return new CreatorLifecycleEventDTO(type, creatorId, null);
    }

    // =========================================================
    // DEACTIVATED
    // =========================================================

    @Test
    void handle_deactivatedEvent_callsDeactivateByCreatorId() {
        UUID creatorId = UUID.randomUUID();

        listener.handle(event("DEACTIVATED", creatorId));

        verify(outfitRepository).deactivateByCreatorId(creatorId);
        verify(outfitRepository, never()).reactivateByCreatorId(any());
        verify(outfitRepository, never()).findAllWithItemsByCreatorId(any());
        verifyNoInteractions(storageService);
    }

    // =========================================================
    // REACTIVATED
    // =========================================================

    @Test
    void handle_reactivatedEvent_callsReactivateByCreatorId() {
        UUID creatorId = UUID.randomUUID();

        listener.handle(event("REACTIVATED", creatorId));

        verify(outfitRepository).reactivateByCreatorId(creatorId);
        verify(outfitRepository, never()).deactivateByCreatorId(any());
        verifyNoInteractions(storageService);
    }

    // =========================================================
    // PURGED
    // =========================================================

    @Test
    void handle_purgedEvent_noOutfits_deletesAllWithEmptyList() {
        UUID creatorId = UUID.randomUUID();
        when(outfitRepository.findAllWithItemsByCreatorId(creatorId)).thenReturn(List.of());

        listener.handle(event("PURGED", creatorId));

        verify(outfitRepository).deleteAll(List.of());
        verifyNoInteractions(storageService);
    }

    @Test
    void handle_purgedEvent_withOutfitImages_deletesAllImages() {
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = new Outfit();
        outfit.setImage1Url("img1.webp");
        outfit.setImage2Url("img2.webp");
        when(outfitRepository.findAllWithItemsByCreatorId(creatorId)).thenReturn(List.of(outfit));

        listener.handle(event("PURGED", creatorId));

        verify(storageService).deleteImage("img1.webp");
        verify(storageService).deleteImage("img2.webp");
        verify(outfitRepository).deleteAll(List.of(outfit));
    }

    @Test
    void handle_purgedEvent_withItems_deletesItemImages() {
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = new Outfit();
        Item item1 = new Item();
        item1.setItemImg("item1.webp");
        Item item2 = new Item();
        item2.setItemImg("item2.webp");
        outfit.getItems().add(item1);
        outfit.getItems().add(item2);
        when(outfitRepository.findAllWithItemsByCreatorId(creatorId)).thenReturn(List.of(outfit));

        listener.handle(event("PURGED", creatorId));

        verify(storageService).deleteImage("item1.webp");
        verify(storageService).deleteImage("item2.webp");
        verify(outfitRepository).deleteAll(List.of(outfit));
    }

    // =========================================================
    // Unknown type
    // =========================================================

    @Test
    void handle_unknownEventType_doesNotCallAnyRepositoryMethod() {
        UUID creatorId = UUID.randomUUID();

        listener.handle(event("UNKNOWN_TYPE", creatorId));

        verify(outfitRepository, never()).deactivateByCreatorId(any());
        verify(outfitRepository, never()).reactivateByCreatorId(any());
        verify(outfitRepository, never()).findAllWithItemsByCreatorId(any());
        verifyNoInteractions(storageService);
    }

    // =========================================================
    // Exception propagation
    // =========================================================

    @Test
    void handle_repositoryThrowsOnDeactivate_rethrowsException() {
        UUID creatorId = UUID.randomUUID();
        doThrow(new RuntimeException("DB failure")).when(outfitRepository).deactivateByCreatorId(creatorId);

        assertThatThrownBy(() -> listener.handle(event("DEACTIVATED", creatorId)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB failure");
    }
}