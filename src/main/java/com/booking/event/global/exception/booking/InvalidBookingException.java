package com.booking.event.global.exception.booking;

public class InvalidBookingException extends  RuntimeException{
   public InvalidBookingException(String message){
    super(message);
  }
}
