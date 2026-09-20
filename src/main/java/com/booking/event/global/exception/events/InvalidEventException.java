package com.booking.event.global.exception.events;

public class InvalidEventException extends  RuntimeException{
   public InvalidEventException(String message){
    super(message);
  }
}
