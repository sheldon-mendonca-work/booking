package com.booking.event.global.dto;

public record ErrorResponseDTO(
  String message,
  Integer status
) {
  
}
