package com.booking.event.booking.dto.booking;

import java.util.List;

public record BookingsByEventResponseDTO( String message,
        Integer statusCode,
        List<BookingResponseObj> data) {
  
}
