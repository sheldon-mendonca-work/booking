package com.booking.event.notification.job;

public record EventUpdateNotificationJob(
        Long eventId,
        Long organizerId) {
}
