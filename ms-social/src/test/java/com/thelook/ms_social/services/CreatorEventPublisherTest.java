package com.thelook.ms_social.services;

import com.thelook.dtos.CreatorLifecycleEventDTO;
import com.thelook.ms_social.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorEventPublisherTest {

    @Mock RabbitTemplate rabbitTemplate;

    @InjectMocks CreatorEventPublisher publisher;

    // =========================================================
    // publishDeactivated
    // =========================================================

    @Test
    void publishDeactivated_sendsDeactivatedEventWithCorrectPayload() {
        UUID creatorId = UUID.randomUUID();

        publisher.publishDeactivated(creatorId);

        ArgumentCaptor<CreatorLifecycleEventDTO> captor = ArgumentCaptor.forClass(CreatorLifecycleEventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CREATOR_EXCHANGE),
                eq(RabbitMQConfig.CREATOR_LIFECYCLE_KEY),
                captor.capture());

        assertThat(captor.getValue().type()).isEqualTo("DEACTIVATED");
        assertThat(captor.getValue().creatorId()).isEqualTo(creatorId);
        assertThat(captor.getValue().userId()).isNull();
    }

    // =========================================================
    // publishReactivated
    // =========================================================

    @Test
    void publishReactivated_sendsReactivatedEventWithCorrectPayload() {
        UUID creatorId = UUID.randomUUID();

        publisher.publishReactivated(creatorId);

        ArgumentCaptor<CreatorLifecycleEventDTO> captor = ArgumentCaptor.forClass(CreatorLifecycleEventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CREATOR_EXCHANGE),
                eq(RabbitMQConfig.CREATOR_LIFECYCLE_KEY),
                captor.capture());

        assertThat(captor.getValue().type()).isEqualTo("REACTIVATED");
        assertThat(captor.getValue().creatorId()).isEqualTo(creatorId);
        assertThat(captor.getValue().userId()).isNull();
    }

    // =========================================================
    // publishPurged
    // =========================================================

    @Test
    void publishPurged_sendsPurgedEventWithCreatorIdAndUserId() {
        UUID creatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        publisher.publishPurged(creatorId, userId);

        ArgumentCaptor<CreatorLifecycleEventDTO> captor = ArgumentCaptor.forClass(CreatorLifecycleEventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CREATOR_EXCHANGE),
                eq(RabbitMQConfig.CREATOR_LIFECYCLE_KEY),
                captor.capture());

        assertThat(captor.getValue().type()).isEqualTo("PURGED");
        assertThat(captor.getValue().creatorId()).isEqualTo(creatorId);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    // =========================================================
    // Resilience — exception swallowed
    // =========================================================

    @Test
    void publish_rabbitTemplateThrows_doesNotPropagateException() {
        UUID creatorId = UUID.randomUUID();
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.publishDeactivated(creatorId))
                .doesNotThrowAnyException();
    }
}