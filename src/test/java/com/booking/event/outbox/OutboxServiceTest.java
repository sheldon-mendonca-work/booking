package com.booking.event.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;
import com.booking.event.notification.queue.JobPayloadCodec;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {
    @Mock
    private OutboxRepository outboxRepository;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new OutboxService(
                outboxRepository,
                new JobPayloadCodec());
        when(outboxRepository.save(any(OutboxEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createBookingConfirmationJobPersistsPendingOutboxRecord() {
        outboxService.createBookingConfirmationJob(
                new BookingConfirmationJob(100L, 1L, 10L));

        ArgumentCaptor<OutboxEntity> outboxCaptor =
                ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        OutboxEntity outbox = outboxCaptor.getValue();
        assertEquals(OutboxEventType.BOOKING_CONFIRMATION, outbox.getEventType());
        assertEquals("100:1:10", outbox.getPayload());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertEquals(0, outbox.getRetryCount());
    }

    @Test
    void createEventUpdateNotificationJobPersistsPendingOutboxRecord() {
        outboxService.createEventUpdateNotificationJob(
                new EventUpdateNotificationJob(1L, 20L));

        ArgumentCaptor<OutboxEntity> outboxCaptor =
                ArgumentCaptor.forClass(OutboxEntity.class);
        verify(outboxRepository).save(outboxCaptor.capture());

        OutboxEntity outbox = outboxCaptor.getValue();
        assertEquals(OutboxEventType.EVENT_UPDATE_NOTIFICATION, outbox.getEventType());
        assertEquals("1:20", outbox.getPayload());
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertEquals(0, outbox.getRetryCount());
    }
}
