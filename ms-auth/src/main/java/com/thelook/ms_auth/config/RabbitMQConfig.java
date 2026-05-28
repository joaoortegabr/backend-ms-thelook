package com.thelook.ms_auth.config;

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

    public static final String CREATOR_EXCHANGE = "ex.thelook.creator";
    public static final String QUEUE_CREATOR_LIFECYCLE = "q.creator.lifecycle.auth";

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
    public Queue creatorLifecycleQueue() {
        return new Queue(QUEUE_CREATOR_LIFECYCLE, true);
    }

    @Bean
    public Binding bindCreatorLifecycle(Queue creatorLifecycleQueue, TopicExchange creatorExchange) {
        return BindingBuilder.bind(creatorLifecycleQueue).to(creatorExchange).with("creator.lifecycle");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}