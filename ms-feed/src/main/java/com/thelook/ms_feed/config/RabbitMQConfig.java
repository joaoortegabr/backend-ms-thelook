package com.thelook.ms_feed.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Injetando o nome da aplicação para criar a fila única
    @Value("${spring.application.name}")
    private String appName;

    // Deve ser IGUAL ao ms-creation
    public static final String OUTFIT_EXCHANGE = "ex.thelook.outfit";
    public static final String QUEUE_FEED_SYNC = "q.feed.sync";
    public static final String QUEUE_OUTFIT_DELETED = "q.outfit.deleted.feed";
    public static final String CREATOR_EXCHANGE = "ex.thelook.creator";
    public static final String QUEUE_CREATOR_LIFECYCLE = "q.creator.lifecycle.feed";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // Você PRECISA declarar o bean da Exchange aqui também para que o Binding funcione
    @Bean
    public TopicExchange outfitExchange() {
        return new TopicExchange(OUTFIT_EXCHANGE);
    }

    @Bean
    public Queue feedSyncQueue() {
        return new Queue(QUEUE_FEED_SYNC, true);
    }

    @Bean
    public Queue statusUpdateQueue() {
        // Isso criará algo como: q.image.status.updated.ms-feed
        // Definimos como durável (true) para manter o padrão
        return new Queue("q.image.status.updated." + appName, true);
    }

    @Bean
    public Binding bindStatusUpdate(Queue statusUpdateQueue, TopicExchange outfitExchange) {
        // Quando o worker terminar a imagem, ele manda para "image.status.updated"
        // e o ms-feed recebe aqui para atualizar o índice do Elasticsearch
        return BindingBuilder.bind(statusUpdateQueue).to(outfitExchange).with("image.status.updated");
    }

    @Bean
    public Binding bindFeed(Queue feedSyncQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(feedSyncQueue)
                .to(outfitExchange)
                .with("feed.sync.#");
    }

    @Bean
    public Queue outfitDeletedQueue() {
        return new Queue(QUEUE_OUTFIT_DELETED, true);
    }

    @Bean
    public Binding bindOutfitDeleted(Queue outfitDeletedQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(outfitDeletedQueue).to(outfitExchange).with("outfit.deleted");
    }

    @Bean
    public TopicExchange creatorExchange() {
        return new TopicExchange(CREATOR_EXCHANGE);
    }

    @Bean
    public Queue creatorLifecycleQueue() {
        return new Queue(QUEUE_CREATOR_LIFECYCLE, true);
    }

    @Bean
    public Binding bindCreatorLifecycle(Queue creatorLifecycleQueue, TopicExchange creatorExchange) {
        return BindingBuilder.bind(creatorLifecycleQueue).to(creatorExchange).with("creator.lifecycle");
    }

}
