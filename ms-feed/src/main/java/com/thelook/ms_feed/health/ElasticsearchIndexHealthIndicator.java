package com.thelook.ms_feed.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import com.thelook.ms_feed.entities.OutfitDocument;

@Component("elasticsearchIndex")
public class ElasticsearchIndexHealthIndicator implements HealthIndicator {

    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchIndexHealthIndicator(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public Health health() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(OutfitDocument.class);
            boolean exists = indexOps.exists();
            if (exists) {
                return Health.up().withDetail("index", "outfits").build();
            }
            return Health.down().withDetail("index", "outfits").withDetail("reason", "index does not exist").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("index", "outfits").build();
        }
    }
}