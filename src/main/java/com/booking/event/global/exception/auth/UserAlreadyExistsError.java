package com.booking.event.global.exception.auth;

public class UserAlreadyExistsError extends RuntimeException {
  public UserAlreadyExistsError(String message){
    super(message);
  }
}
