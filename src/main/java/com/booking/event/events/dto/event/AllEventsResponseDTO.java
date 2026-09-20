package com.booking.event.events.dto.event;

import java.util.List;

public record AllEventsResponseDTO(
  String message,
  Integer statusCode,
  List<EventResponseObj> data
) {
  
}
