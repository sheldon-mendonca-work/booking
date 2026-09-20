package com.booking.event.auth.dto.registerUser;

public record RegisterUserResponseDTO (
  String message,
  Integer statusCode,
  String id,
  String name,
  String email,
  String role,
  String jwt
) {
  
}
