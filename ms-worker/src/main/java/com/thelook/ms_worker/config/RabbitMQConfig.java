package com.thelook.ms_worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String OUTFIT_EXCHANGE  = "ex.thelook.outfit";
    public static final String DLX_EXCHANGE     = "ex.thelook.dlx";
    public static final String QUEUE_IMAGE_HIGH = "q.image.process.high";
    public static final String QUEUE_IMAGE_LOW  = "q.image.process.low";
    public static final String QUEUE_IMAGE_DLQ  = "q.image.failed";

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
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue highPriorityQueue() {
        return QueueBuilder.durable(QUEUE_IMAGE_HIGH)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "image.failed")
                .build();
    }

    @Bean
    public Queue lowPriorityQueue() {
        return QueueBuilder.durable(QUEUE_IMAGE_LOW)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "image.failed")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_IMAGE_DLQ).build();
    }

    @Bean
    public Binding bindHigh(Queue highPriorityQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(highPriorityQueue).to(outfitExchange).with("image.high.#");
    }

    @Bean
    public Binding bindLow(Queue lowPriorityQueue, TopicExchange outfitExchange) {
        return BindingBuilder.bind(lowPriorityQueue).to(outfitExchange).with("image.low.#");
    }

    @Bean
    public Binding bindDlq(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("image.failed");
    }
}
