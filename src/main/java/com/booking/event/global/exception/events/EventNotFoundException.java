package com.booking.event.global.exception.events;

public class EventNotFoundException extends  RuntimeException{
   public EventNotFoundException(String message){
    super(message);
  }
}
