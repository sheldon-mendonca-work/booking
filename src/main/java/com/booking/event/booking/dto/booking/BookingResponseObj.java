package com.booking.event.booking.dto.booking;

import java.time.Instant;

import com.booking.event.booking.constants.BookingStatusEnum;

public record BookingResponseObj(
        Long id,
        Long eventId,
        Long customerId,
        Integer quantity,
        BookingStatusEnum status,
        Instant createdAt,
        Instant updatedAt
) {
}