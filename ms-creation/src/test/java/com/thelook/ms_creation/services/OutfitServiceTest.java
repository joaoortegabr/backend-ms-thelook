package com.thelook.ms_creation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.exceptions.BusinessRuleException;
import com.thelook.exceptions.ResourceNotFoundException;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.entities.OutboxMessage;
import com.thelook.ms_creation.models.dtos.ItemRequest;
import com.thelook.ms_creation.models.dtos.OutfitRequest;
import com.thelook.ms_creation.models.enums.ItemType;
import com.thelook.ms_creation.models.enums.OutfitColor;
import com.thelook.ms_creation.models.enums.OutfitStyle;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import com.thelook.ms_creation.repositories.OutboxRepository;
import com.thelook.ms_creation.repositories.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutfitServiceTest {

    @Mock OutfitRepository outfitRepository;
    @Mock OutboxRepository outboxRepository;
    @Mock ObjectMapper objectMapper;
    @Mock StorageService storageService;
    @Mock OutfitMapper outfitMapper;

    OutfitService outfitService;

    @BeforeEach
    void setUp() {
        outfitService = new OutfitService(outfitRepository, outboxRepository, objectMapper, storageService, outfitMapper);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_quandoEncontrado_retornaOutfit() {
        UUID id = UUID.randomUUID();
        Outfit outfit = outfitWith(id, UUID.randomUUID());
        when(outfitRepository.findById(id)).thenReturn(Optional.of(outfit));

        Outfit result = outfitService.findById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void findById_quandoNaoEncontrado_lancaResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(outfitRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outfitService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createOutfit ──────────────────────────────────────────────────────────

    @Test
    void createOutfit_basico_salvaOutfitERetorna() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitRequest request = new OutfitRequest("Look casual", OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null);
        MultipartFile image1 = mockImage("image1.jpg");

        stubCreateDependencies();

        Outfit result = outfitService.createOutfit(creatorId, request, image1, null, null);

        assertThat(result.getCreatorId()).isEqualTo(creatorId);
        assertThat(result.getTitle()).isEqualTo("Look casual");
        assertThat(result.getStyle()).isEqualTo(OutfitStyle.CASUAL);
        assertThat(result.getImageStatus()).isEqualTo(ImageProcessStatus.PENDING);
        verify(outfitRepository).save(any(Outfit.class));
        verify(outboxRepository).save(any(OutboxMessage.class));
    }

    @Test
    void createOutfit_comSegundaImagem_setaImage2Url() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.FORMAL, Set.of(OutfitColor.WHITE), null);
        stubCreateDependencies();
        when(storageService.saveImage(any(), any(), any(), eq("main_look_2"))).thenReturn("path/image2.jpg");

        Outfit result = outfitService.createOutfit(creatorId, request, mockImage("i1.jpg"), mockImage("i2.jpg"), null);

        assertThat(result.getImage2Url()).isEqualTo("path/image2.jpg");
    }

    @Test
    void createOutfit_comItens_criaItensNoOutfit() throws Exception {
        UUID creatorId = UUID.randomUUID();
        ItemRequest itemReq = new ItemRequest(ItemType.SHIRT, "Camiseta", null, "http://loja.com", null);
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.CASUAL, Set.of(OutfitColor.BLUE), List.of(itemReq));
        stubCreateDependencies();

        Outfit result = outfitService.createOutfit(creatorId, request, mockImage("i1.jpg"), null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getItemName()).isEqualTo("Camiseta");
        assertThat(result.getItems().get(0).getItemType()).isEqualTo(ItemType.SHIRT);
        assertThat(result.getItems().get(0).getImageStatus()).isEqualTo(ImageProcessStatus.PENDING);
    }

    @Test
    void createOutfit_comItemImage_setaItemImg() throws Exception {
        UUID creatorId = UUID.randomUUID();
        ItemRequest itemReq = new ItemRequest(ItemType.SHOES, "Tenis", null, "http://loja.com", null);
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.SPORT, Set.of(OutfitColor.RED), List.of(itemReq));
        stubCreateDependencies();
        when(storageService.saveImage(any(), any(), any(), eq("item_0"))).thenReturn("path/item0.jpg");

        Outfit result = outfitService.createOutfit(creatorId, request, mockImage("i1.jpg"), null, List.of(mockImage("item0.jpg")));

        assertThat(result.getItems().get(0).getItemImg()).isEqualTo("path/item0.jpg");
    }

    @Test
    void createOutfit_salvaOutboxMessageComPayloadCorreto() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null);
        stubCreateDependencies();

        outfitService.createOutfit(creatorId, request, mockImage("i1.jpg"), null, null);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("OUTFIT_CREATED");
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"id\":\"test\"}");
    }

    @Test
    void createOutfit_jsonError_lancaBusinessRuleException() throws Exception {
        UUID creatorId = UUID.randomUUID();
        OutfitRequest request = new OutfitRequest("Look", OutfitStyle.CASUAL, Set.of(OutfitColor.BLACK), null);
        when(storageService.saveImage(any(), any(), any(), anyString())).thenReturn("path/img.jpg");
        when(outfitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outfitMapper.toOutfitSyncDTO(any())).thenReturn(mockSyncDto());
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("json error") {});

        assertThatThrownBy(() -> outfitService.createOutfit(creatorId, request, mockImage("i1.jpg"), null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("serializar");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_quandoProprietario_deletaOutfit() {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, creatorId);
        when(outfitRepository.findById(outfitId)).thenReturn(Optional.of(outfit));

        outfitService.delete(outfitId, creatorId);

        verify(outfitRepository).delete(outfit);
    }

    @Test
    void delete_quandoNaoEncontrado_lancaResourceNotFoundException() {
        UUID outfitId = UUID.randomUUID();
        when(outfitRepository.findById(outfitId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outfitService.delete(outfitId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_quandoNaoProprietario_lancaBusinessRuleException() {
        UUID outfitId = UUID.randomUUID();
        Outfit outfit = outfitWith(outfitId, UUID.randomUUID());
        when(outfitRepository.findById(outfitId)).thenReturn(Optional.of(outfit));

        assertThatThrownBy(() -> outfitService.delete(outfitId, UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negado");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubCreateDependencies() throws JsonProcessingException {
        when(storageService.saveImage(any(), any(), any(), anyString())).thenReturn("path/image.jpg");
        when(outfitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outfitMapper.toOutfitSyncDTO(any())).thenReturn(mockSyncDto());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"id\":\"test\"}");
    }

    private Outfit outfitWith(UUID id, UUID creatorId) {
        Outfit outfit = new Outfit();
        outfit.setId(id);
        outfit.setCreatorId(creatorId);
        return outfit;
    }

    private MultipartFile mockImage(String name) {
        return new MockMultipartFile(name, name, "image/jpeg", new byte[]{1, 2, 3});
    }

    private OutfitSyncDTO mockSyncDto() {
        return new OutfitSyncDTO(UUID.randomUUID(), UUID.randomUUID(), "title",
                null, null, "CASUAL", Set.of("BLACK"), null, ImageProcessStatus.PENDING);
    }
}