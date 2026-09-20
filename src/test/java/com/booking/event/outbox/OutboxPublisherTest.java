package com.booking.event.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.booking.event.notification.queue.RedisQueueNames;
import com.booking.event.notification.queue.RedisQueueService;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {
    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RedisQueueService redisQueueService;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(
                outboxRepository,
                redisQueueService,
                10);
    }

    @Test
    void publishPendingPublishesBookingOutboxRecordAndMarksPublished() {
        OutboxEntity outbox = outbox(
                OutboxEventType.BOOKING_CONFIRMATION,
                "100:1:10");
        when(outboxRepository.findNextPublishableBatch(any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));

        outboxPublisher.publishPending();

        verify(redisQueueService).enqueueSerialized(
                RedisQueueNames.BOOKING_CONFIRMATION_QUEUE,
                "100:1:10");
        assertEquals(OutboxStatus.PUBLISHED, outbox.getStatus());
        assertEquals(1, outbox.getRetryCount());
        assertNull(outbox.getLastError());
    }

    @Test
    void publishPendingPublishesEventUpdateOutboxRecordToEventQueue() {
        OutboxEntity outbox = outbox(
                OutboxEventType.EVENT_UPDATE_NOTIFICATION,
                "1:20");
        when(outboxRepository.findNextPublishableBatch(any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));

        outboxPublisher.publishPending();

        verify(redisQueueService).enqueueSerialized(
                RedisQueueNames.EVENT_UPDATE_QUEUE,
                "1:20");
        assertEquals(OutboxStatus.PUBLISHED, outbox.getStatus());
    }

    @Test
    void publishPendingMarksRecordFailedWhenRedisPublishFails() {
        OutboxEntity outbox = outbox(
                OutboxEventType.BOOKING_CONFIRMATION,
                "100:1:10");
        when(outboxRepository.findNextPublishableBatch(any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        doThrow(new RuntimeException("redis unavailable"))
                .when(redisQueueService)
                .enqueueSerialized(eq(RedisQueueNames.BOOKING_CONFIRMATION_QUEUE), eq("100:1:10"));

        outboxPublisher.publishPending();

        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(1, outbox.getRetryCount());
        assertEquals("redis unavailable", outbox.getLastError());
    }

    private OutboxEntity outbox(
            OutboxEventType eventType,
            String payload) {
        OutboxEntity outbox = new OutboxEntity();
        outbox.setEventType(eventType);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);
        return outbox;
    }
}
