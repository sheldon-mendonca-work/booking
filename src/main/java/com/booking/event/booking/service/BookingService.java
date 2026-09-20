package com.booking.event.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.booking.event.auth.entity.UserEntity;
import com.booking.event.auth.repository.AuthRepository;
import com.booking.event.booking.constants.BookingStatusEnum;
import com.booking.event.booking.dto.booking.AllBookingsResponseDTO;
import com.booking.event.booking.dto.booking.BookingCreateRequestDTO;
import com.booking.event.booking.dto.booking.BookingResponseDTO;
import com.booking.event.booking.dto.booking.BookingResponseObj;
import com.booking.event.booking.dto.booking.BookingsByEventResponseDTO;
import com.booking.event.booking.dto.booking.DeleteBookingResponseDTO;
import com.booking.event.booking.entity.BookingEntity;
import com.booking.event.booking.repository.BookingRepository;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.global.exception.auth.UserDoesNotExistError;
import com.booking.event.global.exception.booking.BookingNotFoundException;
import com.booking.event.global.exception.booking.InvalidBookingException;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.UnauthorizedException;
import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.outbox.OutboxService;

import jakarta.transaction.Transactional;

@Service 
public class BookingService {
  private BookingRepository bookingRepository;
  private EventRepository eventRepository;
  private AuthRepository authRepository;
  private OutboxService outboxService;

  public BookingService(
    BookingRepository bookingRepository,
    EventRepository eventRepository,
    AuthRepository authRepository,
    OutboxService outboxService
  ){
    this.bookingRepository = bookingRepository;
    this.eventRepository = eventRepository;
    this.authRepository = authRepository;
    this.outboxService = outboxService;
  }

  private BookingResponseObj convertBookingToBookingResponseObj(BookingEntity booking){
    return new BookingResponseObj(
                    booking.getId(),
                    booking.getEvent().getId(),
                    booking.getCustomer().getId(),
                    booking.getQuantity(),
                    booking.getStatus(),
                    booking.getCreatedAt(),
                    booking.getUpdatedAt()
            );
  }

  private Authentication getAuthentication() {
    Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
        throw new AuthenticationCredentialsNotFoundException("Authentication is required");
    }

    return authentication;
  }

  private Long getAuthenticatedUserId() {
    try {
        return Long.valueOf(getAuthentication().getName());
    } catch (NumberFormatException exception) {
        throw new UnauthorizedException("Invalid authenticated user");
    }
  }

  private boolean hasRole(String role) {
    Authentication authentication = getAuthentication();

    return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(role::equals);
  }

  private boolean isAdmin() {
    return hasRole("ROLE_ADMIN");
  }

  private boolean isCustomer() {
    return hasRole("ROLE_USER");
  }

  private void requireCustomerRole(String message) {
    if (!isCustomer()) {
        throw new UnauthorizedException(message);
    }
  }

  private void validateQuantity(BookingCreateRequestDTO request) {
    if (request == null || request.quantity() == null || request.quantity() < 1) {
        throw new InvalidBookingException("Quantity must be at least 1");
    }
  }

  private void authorizeBookingRead(BookingEntity booking) {
    Long userId = getAuthenticatedUserId();

    if (isAdmin()
            || booking.getCustomer().getId().equals(userId)
            || booking.getEvent().getOrganizerId().equals(userId)) {
        return;
    }

    throw new UnauthorizedException(
            "You are not authorized to view this booking");
  }

  private void authorizeEventBookingsRead(EventEntity event) {
    Long userId = getAuthenticatedUserId();

    if (isAdmin() || event.getOrganizerId().equals(userId)) {
        return;
    }

    throw new UnauthorizedException(
            "You are not authorized to view bookings for this event");
  }

  public BookingsByEventResponseDTO getBookingsByEventId(Long eventId) {

    EventEntity event = eventRepository.findById(eventId)
            .orElseThrow(() ->
                    new EventNotFoundException("Event not found"));

    authorizeEventBookingsRead(event);

    List<BookingEntity> bookings =
            bookingRepository.findAllByEventIdWithEventAndCustomer(eventId);

    List<BookingResponseObj> bookingList = bookings.stream()
            .map(this::convertBookingToBookingResponseObj)
            .toList();

    return new BookingsByEventResponseDTO(
            "Bookings Fetched Successfully",
            HttpStatus.OK.value(),
            bookingList
    );
}

public AllBookingsResponseDTO getAllBookings() {

    if (!isAdmin()) {
        throw new UnauthorizedException(
                "You are not authorized to view all bookings");
    }

    List<BookingEntity> bookings =
            bookingRepository.findAllWithEventAndCustomer();

    List<BookingResponseObj> bookingList = bookings.stream()
            .map(this::convertBookingToBookingResponseObj)
            .toList();

    return new AllBookingsResponseDTO(
            "Bookings Fetched Successfully",
            HttpStatus.OK.value(),
            bookingList
    );
}

public BookingResponseDTO getBookingById(Long bookingId) {

    BookingEntity booking = bookingRepository.findByIdWithEventAndCustomer(bookingId)
            .orElseThrow(() ->
                    new BookingNotFoundException("Booking not found"));

    authorizeBookingRead(booking);

    BookingResponseObj responseObj =
            convertBookingToBookingResponseObj(booking);

    return new BookingResponseDTO(
            "Booking Fetched Successfully",
            HttpStatus.OK.value(),
            responseObj
    );
}

@Transactional 
public BookingResponseDTO createBookingByEventId(
        Long eventId,
        BookingCreateRequestDTO request) {

    validateQuantity(request);
    requireCustomerRole("Only customers can create bookings");

    Long customerId = getAuthenticatedUserId();
    requireCustomerRole("Only customers can delete bookings");

    EventEntity event = eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() ->
                    new EventNotFoundException("Event not found"));

    if (event.getAvailableTickets() < request.quantity()) {
        throw new InvalidBookingException(
                "Not enough tickets available"
        );
    }

    UserEntity customer = authRepository.findById(customerId)
            .orElseThrow(() ->
                    new UserDoesNotExistError("Customer not found"));

    event.setAvailableTickets(
            event.getAvailableTickets() - request.quantity()
    );

    BookingEntity booking = new BookingEntity();

    booking.setEvent(event);
    booking.setCustomer(customer);
    booking.setQuantity(request.quantity());
    booking.setStatus(BookingStatusEnum.SUCCESSFUL);

    BookingEntity savedBooking = bookingRepository.save(booking);

    outboxService.createBookingConfirmationJob(
            new BookingConfirmationJob(
                    savedBooking.getId(),
                    savedBooking.getEvent().getId(),
                    savedBooking.getCustomer().getId()));

    BookingResponseObj responseObj =
            convertBookingToBookingResponseObj(savedBooking);

    return new BookingResponseDTO(
            "Booking Created Successfully",
            HttpStatus.CREATED.value(),
            responseObj
    );
}

@Transactional
public DeleteBookingResponseDTO deleteBookingById(Long bookingId) {

    Long customerId = getAuthenticatedUserId();
    requireCustomerRole("Only customers can delete bookings");

    BookingEntity booking = bookingRepository.findByIdWithEventAndCustomer(bookingId)
            .orElseThrow(() ->
                    new BookingNotFoundException("Booking not found"));

    // Only the customer who made the booking can delete it
    if (!booking.getCustomer().getId().equals(customerId)) {
        throw new UnauthorizedException(
                "You are not authorized to delete this booking"
        );
    }

    EventEntity event = eventRepository.findByIdForUpdate(
            booking.getEvent().getId()
    ).orElseThrow(() ->
            new EventNotFoundException("Event not found"));

    int restoredTickets = event.getAvailableTickets() + booking.getQuantity();

    if (restoredTickets > event.getCapacity()) {
        throw new InvalidBookingException(
                "Available tickets cannot exceed event capacity");
    }

    event.setAvailableTickets(restoredTickets);

    bookingRepository.delete(booking);

    return new DeleteBookingResponseDTO(
            "Booking Deleted Successfully",
            HttpStatus.OK.value()
    );
}
}
