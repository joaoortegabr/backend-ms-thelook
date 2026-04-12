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

    public static final String QUEUE_IMAGE_HIGH = "q.image.process.high";
    public static final String QUEUE_IMAGE_LOW = "q.image.process.low";
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

    // Filas como Duráveis (true) para não sumirem se o Rabbit reiniciar
    @Bean
    public Queue highPriorityQueue() { return new Queue(QUEUE_IMAGE_HIGH, true); }

    @Bean
    public Queue lowPriorityQueue() { return new Queue(QUEUE_IMAGE_LOW, true); }

    @Bean
    public Queue feedSyncQueue() { return new Queue(QUEUE_FEED_SYNC, true); }

    // Bindings: Vinculando as filas à Topic Exchange usando as chaves (Routing Keys)
    @Bean
    public Binding bindHigh(Queue highPriorityQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(highPriorityQueue).to(outfitExchange).with("image.high.#");
    }

    @Bean
    public Binding bindLow(Queue lowPriorityQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(lowPriorityQueue).to(outfitExchange).with("image.low.#");
    }

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