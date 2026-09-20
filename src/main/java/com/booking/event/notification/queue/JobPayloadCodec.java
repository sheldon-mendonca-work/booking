package com.booking.event.notification.queue;

import org.springframework.stereotype.Component;

import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;

@Component
public class JobPayloadCodec {
    public String serialize(Object payload) {
        if (payload instanceof BookingConfirmationJob job) {
            return String.join(
                    ":",
                    job.bookingId().toString(),
                    job.eventId().toString(),
                    job.customerId().toString());
        }

        if (payload instanceof EventUpdateNotificationJob job) {
            return String.join(
                    ":",
                    job.eventId().toString(),
                    job.organizerId().toString());
        }

        throw new IllegalArgumentException("Unsupported Redis job payload type");
    }

    public <T> T deserialize(String payload, Class<T> payloadType) {
        String[] parts = payload.split(":");

        if (payloadType.equals(BookingConfirmationJob.class)) {
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid booking confirmation job payload");
            }

            return payloadType.cast(new BookingConfirmationJob(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1]),
                    Long.valueOf(parts[2])));
        }

        if (payloadType.equals(EventUpdateNotificationJob.class)) {
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid event update job payload");
            }

            return payloadType.cast(new EventUpdateNotificationJob(
                    Long.valueOf(parts[0]),
                    Long.valueOf(parts[1])));
        }

        throw new IllegalArgumentException("Unsupported Redis job payload type");
    }
}
