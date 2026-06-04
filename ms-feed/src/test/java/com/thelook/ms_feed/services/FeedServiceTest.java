package com.thelook.ms_feed.services;

import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock ElasticsearchOperations elasticsearchOperations;
    @Mock OutfitElasticRepository repository;
    @InjectMocks FeedService feedService;

    @Test
    void findById_quandoEncontrado_retornaDocumento() {
        OutfitDocument doc = new OutfitDocument();
        doc.setId("outfit-1");
        when(repository.findById("outfit-1")).thenReturn(Optional.of(doc));

        Optional<OutfitDocument> result = feedService.findById("outfit-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("outfit-1");
    }

    @Test
    void findById_quandoNaoEncontrado_retornaVazio() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThat(feedService.findById("missing")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCreatorId_delegaParaElasticsearch() {
        UUID creatorId = UUID.randomUUID();
        mockSearchHits();

        SearchHits<OutfitDocument> result = feedService.findByCreatorId(creatorId, 20, null);

        assertThat(result).isNotNull();
        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByCreatorId_comLastSortValues_usaSearchAfterValido() {
        UUID creatorId = UUID.randomUUID();
        mockSearchHits();
        List<Object> cursor = List.of(String.valueOf(System.currentTimeMillis()), UUID.randomUUID().toString());

        feedService.findByCreatorId(creatorId, 10, cursor);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_semFiltros_delegaParaElasticsearch() {
        SearchHits<OutfitDocument> mockHits = mockSearchHits();

        SearchHits<OutfitDocument> result = feedService.searchFeed(null, null, null, null, 20, null);

        assertThat(result).isNotNull();
        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comStyle_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed("CASUAL", null, null, null, 20, null);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comColors_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed(null, List.of("RED", "BLUE"), null, null, 20, null);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comItemType_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed(null, null, "SHOES", null, 20, null);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comTitle_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed(null, null, null, "verao", 20, null);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comTodosFiltros_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed("CASUAL", List.of("RED"), "SHOES", "verao", 10, null);

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_comLastSortValues_delegaParaElasticsearch() {
        mockSearchHits();

        feedService.searchFeed(null, null, null, null, 20, List.of(String.valueOf(System.currentTimeMillis()), UUID.randomUUID().toString()));

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFeed_filtrosEmBranco_naoAplicaFiltros() {
        mockSearchHits();

        feedService.searchFeed("", List.of(), "", "", 20, List.of());

        verify(elasticsearchOperations).search(any(Query.class), eq(OutfitDocument.class));
    }

    @SuppressWarnings("unchecked")
    private SearchHits<OutfitDocument> mockSearchHits() {
        SearchHits<OutfitDocument> mockHits = mock(SearchHits.class);
        when(elasticsearchOperations.search(any(Query.class), eq(OutfitDocument.class))).thenReturn(mockHits);
        return mockHits;
    }
}