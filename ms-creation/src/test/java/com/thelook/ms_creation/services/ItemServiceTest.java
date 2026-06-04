package com.thelook.ms_creation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.ItemResponse;
import com.thelook.ms_creation.models.enums.ItemType;
import com.thelook.ms_creation.models.mappers.ItemMapper;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.repositories.ItemRepository;
import com.thelook.ms_creation.repositories.OutboxRepository;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock ItemRepository itemRepository;
    @Mock OutfitRepository outfitRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock StorageService storageService;
    @Mock ItemMapper itemMapper;
    @Mock OutfitMapper outfitMapper;
    @Mock ObjectMapper objectMapper;
    @Mock ApplicationEventPublisher eventPublisher;

    ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, outfitRepository, outboxRepository,
                storageService, itemMapper, outfitMapper, objectMapper, eventPublisher);
    }

    // ── addItem ────────────────────────────────────────────────────────────────

    @Test
    void addItem_outfitNotFound_throwsResourceNotFoundException() {
        UUID outfitId = UUID.randomUUID();
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addItem(outfitId, UUID.randomUUID(), itemRequest(), null))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void addItem_notOwner_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, UUID.randomUUID());
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        assertThatThrownBy(() -> itemService.addItem(outfitId, UUID.randomUUID(), itemRequest(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negado");

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void addItem_itemLimitReached_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        for (int i = 0; i < 6; i++) outfit.addItem(new Item());
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));

        assertThatThrownBy(() -> itemService.addItem(outfitId, creatorId, itemRequest(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Limite");

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void addItem_semImagem_adicionaItemComItemImgVazio() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        stubPublishDependencies(outfitId);
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));
        when(outfitRepository.save(any())).thenReturn(outfit);
        when(itemMapper.toItemResponse(any())).thenReturn(itemResponse());

        itemService.addItem(outfitId, creatorId, itemRequest(), null);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).toItemResponse(captor.capture());
        assertThat(captor.getValue().getItemImg()).isEqualTo("");
        verify(storageService, never()).saveImage(any(), any(), any(), anyString());
    }

    @Test
    void addItem_comImagem_salvaImagemESetaItemImg() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        MockMultipartFile image = new MockMultipartFile("img", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        stubPublishDependencies(outfitId);
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));
        when(outfitRepository.save(any())).thenReturn(outfit);
        when(storageService.saveImage(any(), any(), any(), anyString())).thenReturn("path/item.jpg");
        when(itemMapper.toItemResponse(any())).thenReturn(itemResponse());

        itemService.addItem(outfitId, creatorId, itemRequest(), image);

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemMapper).toItemResponse(captor.capture());
        assertThat(captor.getValue().getItemImg()).isEqualTo("path/item.jpg");
        verify(storageService).saveImage(eq(image), eq(creatorId), eq(outfitId), anyString());
    }

    @Test
    void addItem_sucesso_publicaOutfitUpdatedNoOutbox() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        stubPublishDependencies(outfitId);
        when(outfitRepository.findWithItemsById(outfitId)).thenReturn(Optional.of(outfit));
        when(outfitRepository.save(any())).thenReturn(outfit);
        when(itemMapper.toItemResponse(any())).thenReturn(itemResponse());

        itemService.addItem(outfitId, creatorId, itemRequest(), null);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OUTFIT_UPDATED");
        assertThat(captor.getValue().getAggregateId()).isEqualTo(outfitId.toString());
    }

    // ── deleteItem ─────────────────────────────────────────────────────────────

    @Test
    void deleteItem_itemNotFound_throwsResourceNotFoundException() {
        when(itemRepository.findWithOutfitById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.deleteItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void deleteItem_outfitMismatch_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit wrongOutfit = outfitWith(UUID.randomUUID(), creatorId);
        Item item = itemWithOutfit(itemId, wrongOutfit);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.deleteItem(outfitId, itemId, creatorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pertence");

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void deleteItem_notOwner_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID actualOwner = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, actualOwner);
        Item item = itemWithOutfit(itemId, outfit);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.deleteItem(outfitId, itemId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negado");

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void deleteItem_sucesso_removeItemEPublicaOutfitUpdated() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        Item item = itemWithOutfit(itemId, outfit);
        stubPublishDependencies(outfitId);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));
        when(outfitRepository.save(any())).thenReturn(outfit);

        itemService.deleteItem(outfitId, itemId, creatorId);

        verify(outfitRepository).save(outfit);
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OUTFIT_UPDATED");
    }

    // ── hardDeleteItem ─────────────────────────────────────────────────────────

    @Test
    void hardDeleteItem_itemNotFound_throwsResourceNotFoundException() {
        when(itemRepository.findWithOutfitById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.hardDeleteItem(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void hardDeleteItem_outfitMismatch_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Outfit wrongOutfit = outfitWith(UUID.randomUUID(), UUID.randomUUID());
        Item item = itemWithOutfit(itemId, wrongOutfit);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.hardDeleteItem(outfitId, itemId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pertence");
    }

    @Test
    void hardDeleteItem_notOwner_throwsBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, UUID.randomUUID());
        Item item = itemWithOutfit(itemId, outfit);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.hardDeleteItem(outfitId, itemId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negado");
    }

    @Test
    void hardDeleteItem_sucesso_deletaItemDiretamenteELimpaStorage() throws Exception {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        Item item = itemWithOutfit(itemId, outfit);
        item.setItemImg("path/item.jpg");
        stubPublishDependencies(outfitId);
        when(itemRepository.findWithOutfitById(itemId)).thenReturn(Optional.of(item));

        itemService.hardDeleteItem(outfitId, itemId, creatorId);

        verify(storageService).deleteImage("path/item.jpg");
        verify(itemRepository).delete(item);
        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OUTFIT_UPDATED");
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private void stubPublishDependencies(UUID outfitId) throws JsonProcessingException {
        when(outfitMapper.toOutfitSyncDTO(any())).thenReturn(mockSyncDto(outfitId));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":true}");
    }

    private Outfit outfitWith(UUID id, UUID creatorId) {
        Outfit outfit = new Outfit();
        outfit.setId(id);
        outfit.setCreatorId(creatorId);
        return outfit;
    }

    private Item itemWithOutfit(UUID itemId, Outfit outfit) {
        Item item = new Item();
        item.setId(itemId);
        outfit.addItem(item);
        return item;
    }

    private ItemRequest itemRequest() {
        return new ItemRequest(ItemType.SHIRT, "Camiseta", null, "http://loja.com", null);
    }

    private ItemResponse itemResponse() {
        return new ItemResponse(UUID.randomUUID(), ItemType.SHIRT, "Camiseta", "", "http://loja.com",
                ImageProcessStatus.PENDING);
    }

    private OutfitSyncDTO mockSyncDto(UUID outfitId) {
        return new OutfitSyncDTO(outfitId, UUID.randomUUID(), "Look",
                null, null, "CASUAL", Set.of("BLACK"), null, ImageProcessStatus.PENDING);
    }
}
