package com.thelook.ms_social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    // Dead-letter exchange e fila para mensagens que falharam no processamento
    public static final String DLX_OUTFIT             = "ex.thelook.outfit.dlx";
    public static final String DLQ_OUTFIT_DELETED     = "q.outfit.deleted.social.dlq";

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
    public DirectExchange outfitDlx() {
        return new DirectExchange(DLX_OUTFIT);
    }

    @Bean
    public Queue outfitDeletedDlq() {
        return new Queue(DLQ_OUTFIT_DELETED, true);
    }

    @Bean
    public Binding bindOutfitDeletedDlq(Queue outfitDeletedDlq, DirectExchange outfitDlx) {
        return BindingBuilder.bind(outfitDeletedDlq).to(outfitDlx).with(QUEUE_OUTFIT_DELETED);
    }

    @Bean
    public Queue outfitDeletedQueue() {
        // Requer que a fila anterior seja deletada no RabbitMQ para recriação com DLX
        return QueueBuilder.durable(QUEUE_OUTFIT_DELETED)
                .deadLetterExchange(DLX_OUTFIT)
                .deadLetterRoutingKey(QUEUE_OUTFIT_DELETED)
                .build();
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
