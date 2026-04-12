package com.thelook.ms_feed.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("elasticsearch:9200")
                // .withBasicAuth("user", "pass") // Remova se desativou a segurança
                .build();
    }
}