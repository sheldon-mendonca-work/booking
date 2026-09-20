package com.booking.event.outbox;

import org.springframework.stereotype.Service;

import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;
import com.booking.event.notification.queue.JobPayloadCodec;

@Service
public class OutboxService {
    private final OutboxRepository outboxRepository;
    private final JobPayloadCodec jobPayloadCodec;

    public OutboxService(
            OutboxRepository outboxRepository,
            JobPayloadCodec jobPayloadCodec) {
        this.outboxRepository = outboxRepository;
        this.jobPayloadCodec = jobPayloadCodec;
    }

    public OutboxEntity createBookingConfirmationJob(
            BookingConfirmationJob job) {
        return create(
                OutboxEventType.BOOKING_CONFIRMATION,
                jobPayloadCodec.serialize(job));
    }

    public OutboxEntity createEventUpdateNotificationJob(
            EventUpdateNotificationJob job) {
        return create(
                OutboxEventType.EVENT_UPDATE_NOTIFICATION,
                jobPayloadCodec.serialize(job));
    }

    private OutboxEntity create(
            OutboxEventType eventType,
            String payload) {
        OutboxEntity outbox = new OutboxEntity();
        outbox.setEventType(eventType);
        outbox.setPayload(payload);
        outbox.setStatus(OutboxStatus.PENDING);
        outbox.setRetryCount(0);

        return outboxRepository.save(outbox);
    }
}
