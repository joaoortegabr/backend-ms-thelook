package com.thelook.ms_worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // USAR A MESMA EXCHANGE DO CREATION
    public static final String OUTFIT_EXCHANGE = "ex.thelook.outfit";
    public static final String QUEUE_IMAGE_HIGH = "q.image.process.high";
    public static final String QUEUE_IMAGE_LOW = "q.image.process.low";

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
    public Queue highPriorityQueue() {
        return new Queue(QUEUE_IMAGE_HIGH, true); // Identica à do creation
    }

    @Bean
    public Queue lowPriorityQueue() {
        return new Queue(QUEUE_IMAGE_LOW, true);
    }

    @Bean
    public Binding bindHigh(Queue highPriorityQueue, TopicExchange outfitExchange) {
        // Usando a mesma Routing Key que o creation usa para enviar
        return BindingBuilder.bind(highPriorityQueue).to(outfitExchange).with("image.high.#");
    }

    @Bean
    public Binding bindLow(Queue lowPriorityQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(lowPriorityQueue).to(outfitExchange).with("image.low.#");
    }
}
