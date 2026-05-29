package com.thelook.ms_creation.config;

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

    @Value("${spring.application.name}")
    private String appName;

    public static final String OUTFIT_EXCHANGE = "ex.thelook.outfit";
    public static final String CREATOR_EXCHANGE = "ex.thelook.creator";
    public static final String QUEUE_CREATOR_LIFECYCLE = "q.creator.lifecycle.creation";

    public static final String QUEUE_FEED_SYNC = "q.feed.sync";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public TopicExchange outfitExchange() {
        return new TopicExchange(OUTFIT_EXCHANGE);
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

    @Bean
    public Queue feedSyncQueue() { return new Queue(QUEUE_FEED_SYNC, true); }

    @Bean
    public Binding bindFeed(Queue feedSyncQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(feedSyncQueue).to(outfitExchange).with("feed.sync.#");
    }

    @Bean
    public Queue statusUpdateQueue() {
        // Gera: q.image.status.updated.ms-creation
        return new Queue("q.image.status.updated." + appName, true);
    }

    @Bean
    public Binding bindStatusUpdate(Queue statusUpdateQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(statusUpdateQueue).to(outfitExchange).with("image.status.updated");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}