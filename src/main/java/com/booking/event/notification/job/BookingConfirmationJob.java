package com.booking.event.notification.job;

public record BookingConfirmationJob(
        Long bookingId,
        Long eventId,
        Long customerId) {
}
