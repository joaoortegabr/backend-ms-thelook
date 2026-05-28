package com.thelook.ms_social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CREATOR_EXCHANGE       = "ex.thelook.creator";
    public static final String OUTFIT_EXCHANGE        = "ex.thelook.outfit";
    public static final String CREATOR_LIFECYCLE_KEY  = "creator.lifecycle";
    public static final String QUEUE_OUTFIT_DELETED   = "q.outfit.deleted.social";

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public TopicExchange creatorExchange() {
        return new TopicExchange(CREATOR_EXCHANGE);
    }

    @Bean
    public TopicExchange outfitExchange() {
        return new TopicExchange(OUTFIT_EXCHANGE);
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
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}