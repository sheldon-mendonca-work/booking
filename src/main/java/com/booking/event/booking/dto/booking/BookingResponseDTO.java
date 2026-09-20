package com.booking.event.booking.dto.booking;

public record BookingResponseDTO(
        String message,
        Integer statusCode,
        BookingResponseObj data
) {
}