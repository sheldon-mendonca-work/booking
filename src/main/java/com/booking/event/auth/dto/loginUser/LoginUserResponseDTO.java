package com.booking.event.auth.dto.loginUser;

public record LoginUserResponseDTO (
  String message,
  Integer statusCode,
  String id,
  String name,
  String email,
  String role,
  String jwt
) {
  
}
