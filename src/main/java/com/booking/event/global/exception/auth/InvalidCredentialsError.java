package com.booking.event.global.exception.auth;

public class InvalidCredentialsError extends  RuntimeException{
   public InvalidCredentialsError(String message){
    super(message);
  }
}
