package com.thelook.ms_creation.services;

import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_creation.config.RabbitMQConfig;
import com.thelook.ms_creation.entities.Item;
import com.thelook.ms_creation.entities.Outfit;
import com.thelook.ms_creation.models.enums.ItemType;
import com.thelook.ms_creation.models.mappers.OutfitMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutfitEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;
    @Mock OutfitMapper outfitMapper;

    OutfitEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OutfitEventPublisher(rabbitTemplate, outfitMapper);
    }

    @Test
    void publishOutfitCreated_semItens_enviaFeedSyncEHighPriority() {
        Outfit outfit = outfitSemItens();
        when(outfitMapper.toOutfitSyncDTO(outfit)).thenReturn(mockSyncDto(outfit.getId()));

        publisher.publishOutfitCreated(outfit);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.OUTFIT_EXCHANGE, "feed.sync.created", outfitMapper.toOutfitSyncDTO(outfit));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.high.process"), any(Map.class));
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.low.process"), any());
    }

    @Test
    void publishOutfitCreated_comItens_enviaLowPriorityTambem() {
        Outfit outfit = outfitComItens();
        when(outfitMapper.toOutfitSyncDTO(outfit)).thenReturn(mockSyncDto(outfit.getId()));

        publisher.publishOutfitCreated(outfit);

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.low.process"), any(Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOutfitCreated_semImage2_highPriorityUsaStringVazia() {
        Outfit outfit = outfitSemItens();
        outfit.setImage2Url(null);
        when(outfitMapper.toOutfitSyncDTO(outfit)).thenReturn(mockSyncDto(outfit.getId()));

        publisher.publishOutfitCreated(outfit);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.high.process"), captor.capture());

        List<String> images = (List<String>) captor.getValue().get("images");
        assertThat(images).contains("");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOutfitCreated_comImage2_highPriorityIncluiImage2Url() {
        Outfit outfit = outfitSemItens();
        outfit.setImage2Url("path/image2.jpg");
        when(outfitMapper.toOutfitSyncDTO(outfit)).thenReturn(mockSyncDto(outfit.getId()));

        publisher.publishOutfitCreated(outfit);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.high.process"), captor.capture());

        List<String> images = (List<String>) captor.getValue().get("images");
        assertThat(images).contains("path/image2.jpg");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOutfitCreated_comItens_lowPriorityContemPathsDosItens() {
        Outfit outfit = outfitComItens();
        when(outfitMapper.toOutfitSyncDTO(outfit)).thenReturn(mockSyncDto(outfit.getId()));

        publisher.publishOutfitCreated(outfit);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.OUTFIT_EXCHANGE), eq("image.low.process"), captor.capture());

        List<String> itemImages = (List<String>) captor.getValue().get("itemImages");
        assertThat(itemImages).containsExactly("path/item1.jpg");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Outfit outfitSemItens() {
        Outfit outfit = new Outfit();
        outfit.setId(UUID.randomUUID());
        outfit.setImage1Url("path/image1.jpg");
        return outfit;
    }

    private Outfit outfitComItens() {
        Outfit outfit = outfitSemItens();
        Item item = new Item();
        item.setItemType(ItemType.SHIRT);
        item.setItemName("Camiseta");
        item.setItemImg("path/item1.jpg");
        outfit.getItems().add(item);
        return outfit;
    }

    private OutfitSyncDTO mockSyncDto(UUID outfitId) {
        return new OutfitSyncDTO(outfitId, UUID.randomUUID(), "Look", null, null,
                "CASUAL", Set.of("BLACK"), null, ImageProcessStatus.PENDING);
    }
}