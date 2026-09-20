package com.booking.event.global.exception.auth;

public class UserDoesNotExistError extends RuntimeException {
  public UserDoesNotExistError(String message){
    super(message);
  }
}
