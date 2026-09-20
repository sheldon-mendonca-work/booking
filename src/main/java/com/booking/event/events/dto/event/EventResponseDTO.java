package com.booking.event.events.dto.event;

public record EventResponseDTO(
  String message,
  Integer statusCode,
  EventResponseObj data
) {
  
}
