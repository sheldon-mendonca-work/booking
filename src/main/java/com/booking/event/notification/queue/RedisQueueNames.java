package com.booking.event.notification.queue;

public final class RedisQueueNames {
    public static final String BOOKING_CONFIRMATION_QUEUE =
            "booking-confirmation-queue";
    public static final String EVENT_UPDATE_QUEUE =
            "event-update-queue";

    private RedisQueueNames() {
    }
}
