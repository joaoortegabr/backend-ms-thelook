package com.thelook.ms_worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

    @Bean
    public SimpleRabbitListenerContainerFactory highPriorityContainerFactory(ConnectionFactory connectionFactory) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("worker-high-");
        executor.initialize();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(8);
        factory.setPrefetchCount(4);
        factory.setTaskExecutor(executor);
        return factory;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory lowPriorityContainerFactory(ConnectionFactory connectionFactory) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("worker-low-");
        executor.initialize();

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        factory.setPrefetchCount(2);
        factory.setTaskExecutor(executor);
        return factory;
    }
}
