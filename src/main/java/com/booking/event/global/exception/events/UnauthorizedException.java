package com.booking.event.global.exception.events;

public class UnauthorizedException extends  RuntimeException{
   public UnauthorizedException(String message){
    super(message);
  }
}
