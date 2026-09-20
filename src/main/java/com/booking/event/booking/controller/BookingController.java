package com.booking.event.booking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.booking.event.booking.dto.booking.AllBookingsResponseDTO;
import com.booking.event.booking.dto.booking.BookingCreateRequestDTO;
import com.booking.event.booking.dto.booking.BookingResponseDTO;
import com.booking.event.booking.dto.booking.BookingsByEventResponseDTO;
import com.booking.event.booking.dto.booking.DeleteBookingResponseDTO;
import com.booking.event.booking.service.BookingService;

import jakarta.validation.Valid;

@RestController
public class BookingController {
  private BookingService bookingService;

  public BookingController(BookingService service) {
    this.bookingService = service;
  }

  @GetMapping("/events/{eventId}/bookings")
  public ResponseEntity<BookingsByEventResponseDTO> getBookingsByEventId(@PathVariable Long eventId) {
    return ResponseEntity.ok(bookingService.getBookingsByEventId(eventId));
  }

  @GetMapping("/bookings")
  public ResponseEntity<AllBookingsResponseDTO> getBookings() {
    return ResponseEntity.ok(bookingService.getAllBookings());
  }

  @GetMapping("/bookings/{id}")
  public ResponseEntity<BookingResponseDTO> getBookingById(@PathVariable Long id) {
    return ResponseEntity.ok(bookingService.getBookingById(id));
  }

  @PostMapping({"/events/{eventId}/bookings", "/bookings/{eventId}"})
  public ResponseEntity<BookingResponseDTO> createBooking(@PathVariable Long eventId,
      @Valid @RequestBody BookingCreateRequestDTO request) {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(bookingService.createBookingByEventId(eventId, request));
  }

  @DeleteMapping("/bookings/{id}")
  public ResponseEntity<DeleteBookingResponseDTO> deleteBooking(
      @PathVariable Long id) {

    return ResponseEntity.ok(
        bookingService.deleteBookingById(id));
  }
}
