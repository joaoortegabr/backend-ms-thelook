package com.thelook.ms_feed.services;

import com.thelook.dtos.ItemSyncDTO;
import com.thelook.dtos.OutfitSyncDTO;
import com.thelook.enums.ImageProcessStatus;
import com.thelook.ms_feed.entities.ItemDocument;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutfitIndexServiceTest {

    @Mock OutfitElasticRepository repository;
    @InjectMocks OutfitIndexService outfitIndexService;

    @Test
    void index_semItens_salvaDocumentoComCamposCorretos() {
        UUID outfitId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        OutfitSyncDTO dto = new OutfitSyncDTO(
                outfitId, creatorId, "Look de verao", "url1.jpg", "url2.jpg",
                "CASUAL", Set.of("RED"), null, ImageProcessStatus.PENDING
        );

        outfitIndexService.index(dto);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(repository).save(captor.capture());

        OutfitDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(outfitId.toString());
        assertThat(saved.getCreatorId()).isEqualTo(creatorId);
        assertThat(saved.getTitle()).isEqualTo("Look de verao");
        assertThat(saved.getStyle()).isEqualTo("CASUAL");
        assertThat(saved.getColors()).containsExactly("RED");
        assertThat(saved.getImage1Url()).isEqualTo("url1.jpg");
        assertThat(saved.getImage2Url()).isEqualTo("url2.jpg");
        assertThat(saved.getImageStatus()).isEqualTo("PENDING");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems()).isEmpty();
    }

    @Test
    void index_comItens_mapeiaItensCorretamente() {
        UUID outfitId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        ItemSyncDTO item = new ItemSyncDTO(itemId, "Camiseta", "SHIRT", "img.jpg", "http://loja.com", ImageProcessStatus.READY);
        OutfitSyncDTO dto = new OutfitSyncDTO(
                outfitId, UUID.randomUUID(), "Look", "url1.jpg", null,
                "CASUAL", Set.of("WHITE"), List.of(item), ImageProcessStatus.READY
        );

        outfitIndexService.index(dto);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(repository).save(captor.capture());

        List<ItemDocument> items = captor.getValue().getItems();
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getItemId()).isEqualTo(itemId.toString());
        assertThat(items.get(0).getItemName()).isEqualTo("Camiseta");
        assertThat(items.get(0).getItemType()).isEqualTo("SHIRT");
        assertThat(items.get(0).getItemImg()).isEqualTo("img.jpg");
        assertThat(items.get(0).getItemUrl()).isEqualTo("http://loja.com");
        assertThat(items.get(0).getImageStatus()).isEqualTo("READY");
    }

    @Test
    void index_comVariosItens_mapeiaTodasAsEntradas() {
        ItemSyncDTO item1 = new ItemSyncDTO(UUID.randomUUID(), "Calca", "PANTS", "c.jpg", "http://a.com", ImageProcessStatus.PENDING);
        ItemSyncDTO item2 = new ItemSyncDTO(UUID.randomUUID(), "Sapato", "SHOES", "s.jpg", "http://b.com", ImageProcessStatus.READY);
        OutfitSyncDTO dto = new OutfitSyncDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Look", "url1.jpg", null,
                "CASUAL", Set.of("BLACK"), List.of(item1, item2), ImageProcessStatus.PENDING
        );

        outfitIndexService.index(dto);

        ArgumentCaptor<OutfitDocument> captor = ArgumentCaptor.forClass(OutfitDocument.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getItems()).hasSize(2);
    }
}