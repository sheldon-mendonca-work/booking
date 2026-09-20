package com.booking.event.global.exception.booking;

public class BookingNotFoundException extends RuntimeException{
  public BookingNotFoundException(String message){
    super(message);
  }
}
