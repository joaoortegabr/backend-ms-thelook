package com.thelook.ms_feed.services;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.thelook.ms_feed.entities.OutfitDocument;
import com.thelook.ms_feed.repositories.OutfitElasticRepository;
import com.thelook.ms_feed.validation.SearchAfterValidator;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FeedService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final OutfitElasticRepository repository;

    public FeedService(ElasticsearchOperations elasticsearchOperations, OutfitElasticRepository repository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.repository = repository;
    }

    public Optional<OutfitDocument> findById(String outfitId) {
        return repository.findById(outfitId);
    }

    public SearchHits<OutfitDocument> findByCreatorId(UUID creatorId, int size, List<Object> lastSortValues) {
        SearchAfterValidator.validate(lastSortValues);
        var query = QueryBuilders.bool(b -> b
                .must(QueryBuilders.term(t -> t.field("creatorId").value(creatorId.toString())))
                .mustNot(QueryBuilders.term(t -> t.field("isActive").value(false)))
        );
        var queryBuilder = new NativeQueryBuilder()
                .withMaxResults(size)
                .withQuery(query)
                .withSort(Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        if (lastSortValues != null && !lastSortValues.isEmpty()) {
            queryBuilder.withSearchAfter(lastSortValues);
        }

        return elasticsearchOperations.search(queryBuilder.build(), OutfitDocument.class);
    }

    public SearchHits<OutfitDocument> searchFeed(
            String style,
            List<String> colors,
            String itemType,
            String title,
            int size,
            List<Object> lastSortValues) {

        SearchAfterValidator.validate(lastSortValues);

        var queryBuilder = new NativeQueryBuilder()
                .withMaxResults(size);

        var boolQuery = QueryBuilders.bool();

        if (style != null && !style.isBlank()) {
            boolQuery.must(q -> q.term(t -> t.field("style").value(style)));
        }

        if (colors != null && !colors.isEmpty()) {
            boolQuery.must(q -> q.terms(t -> t.field("colors")
                    .terms(v -> v.value(colors.stream().map(FieldValue::of).toList()))));
        }

        if (itemType != null && !itemType.isBlank()) {
            boolQuery.must(q -> q.nested(n -> n
                    .path("items")
                    .query(qq -> qq.term(t -> t.field("items.itemType").value(itemType)))
            ));
        }

        if (title != null && !title.isBlank()) {
            boolQuery.must(q -> q.match(m -> m.field("title").query(title)));
        }

        boolQuery.must(q -> q.term(t -> t.field("imageStatus").value("READY")));
        boolQuery.mustNot(q -> q.term(t -> t.field("isActive").value(false)));

        queryBuilder.withQuery(boolQuery.build()._toQuery());

        // 2. Ordenação e Paginação (Search After)
        // Ordenamos por data (mais recentes) e ID (para desempate e consistência)
        queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        if (lastSortValues != null && !lastSortValues.isEmpty()) {
            queryBuilder.withSearchAfter(lastSortValues);
        }

        return elasticsearchOperations.search(queryBuilder.build(), OutfitDocument.class);
    }
}
