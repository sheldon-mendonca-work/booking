package com.booking.event.events.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.booking.event.auth.entity.UserEntity;
import com.booking.event.auth.repository.AuthRepository;
import com.booking.event.events.dto.event.AllEventsResponseDTO;
import com.booking.event.events.dto.event.CreateEventRequestDTO;
import com.booking.event.events.dto.event.DeleteEventResponseDTO;
import com.booking.event.events.dto.event.EventResponseDTO;
import com.booking.event.events.dto.event.EventResponseObj;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.InvalidEventException;
import com.booking.event.global.exception.events.UnauthorizedException;
import com.booking.event.notification.job.EventUpdateNotificationJob;
import com.booking.event.outbox.OutboxService;

@Service
public class EventService {
  private EventRepository eventRepository;
  private AuthRepository authRepository;
  private OutboxService outboxService;

  public EventService(
      EventRepository eventRepository,
      AuthRepository authRepository,
      OutboxService outboxService) {
    this.eventRepository = eventRepository;
    this.authRepository = authRepository;
    this.outboxService = outboxService;
  }

  private EventResponseObj convertEventAndOrganizerToEventResponse(EventEntity event, UserEntity organizer) {
    return new EventResponseObj(
        event.getId(),
        organizer.getName(),
        event.getName(),
        event.getDescription(),
        event.getVenue(),
        event.getStartTime(),
        event.getEndTime(),
        event.getCapacity(),
        event.getAvailableTickets(),
        event.getCreatedAt());
  }

  public AllEventsResponseDTO getAllEvents() {

    List<EventEntity> events = eventRepository.findAll();

    Set<Long> organizerIds = events.stream()
        .map(EventEntity::getOrganizerId)
        .collect(Collectors.toSet());

    Map<Long, UserEntity> organizers = authRepository
        .findAllByIdIn(organizerIds)
        .stream()
        .collect(Collectors.toMap(
            UserEntity::getId,
            user -> user));

    List<EventResponseObj> eventsList = events.stream()
        .map(event -> {
          UserEntity organizer = organizers.get(event.getOrganizerId());
          return convertEventAndOrganizerToEventResponse(event, organizer);
        })
        .toList();
    AllEventsResponseDTO response = new AllEventsResponseDTO("Events Fetched Successfully",
        HttpStatus.OK.value(), eventsList);
    return response;
  }

  private void checkIfStartAfterEnd(CreateEventRequestDTO request) {
    // Business validation
    if (!request.endTime().isAfter(request.startTime())) {
      throw new InvalidEventException("End time must be after start time");
    }
  }

  private Authentication getAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

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
    return getAuthentication().getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(role::equals);
  }

  private void requireAdminRole(String message) {
    if (!hasRole("ROLE_ADMIN")) {
      throw new UnauthorizedException(message);
    }
  }

  public EventResponseDTO createEvent(CreateEventRequestDTO request) {

    checkIfStartAfterEnd(request);
    requireAdminRole("Only admins can create events");

    // Get authenticated organizer
    Long organizerId = getAuthenticatedUserId();

    // Create entity
    EventEntity event = new EventEntity();

    event.setOrganizerId(organizerId);
    event.setName(request.name());
    event.setDescription(request.description());
    event.setVenue(request.venue());
    event.setStartTime(request.startTime());
    event.setEndTime(request.endTime());
    event.setCapacity(request.capacity());

    // Server-controlled
    event.setAvailableTickets(request.capacity());

    // Save
    EventEntity savedEvent = eventRepository.save(event);

    // Get organizer details if response needs organizer name
    UserEntity organizer = authRepository
        .findById(savedEvent.getOrganizerId())
        .orElseThrow(() -> new RuntimeException("Organizer not found"));

    // Build response object
    EventResponseObj responseObj = convertEventAndOrganizerToEventResponse(savedEvent, organizer);

    // Wrap response
    return new EventResponseDTO(
        "Event Created Successfully",
        HttpStatus.CREATED.value(),
        responseObj);
  }

  public EventResponseDTO getEventById(Long requestId) {

    EventEntity event = eventRepository.findById(requestId)
        .orElseThrow(() -> new EventNotFoundException("Event not found"));

    UserEntity organizer = authRepository
        .findById(event.getOrganizerId())
        .orElseThrow(() -> new RuntimeException("Organizer not found"));

    EventResponseObj responseObj = convertEventAndOrganizerToEventResponse(event, organizer);

    return new EventResponseDTO(
        "Event Fetched Successfully",
        HttpStatus.OK.value(),
        responseObj);
  }

  @Transactional
  public EventResponseDTO editEventById(
      Long eventId,
      CreateEventRequestDTO request) {

    EventEntity event = eventRepository.findById(eventId)
        .orElseThrow(() -> new EventNotFoundException("Event not found"));

    requireAdminRole("Only admins can update events");

    Long organizerId = getAuthenticatedUserId();

    // Only the event owner can edit it
    if (!event.getOrganizerId().equals(organizerId)) {
      throw new UnauthorizedException(
          "You are not authorized to edit this event");
    }

    // Validate event timing
    if (!request.endTime().isAfter(request.startTime())) {
      throw new InvalidEventException(
          "End time must be after start time");
    }

    event.setName(request.name());
    event.setDescription(request.description());
    event.setVenue(request.venue());
    event.setStartTime(request.startTime());
    event.setEndTime(request.endTime());

    /*
     * Capacity change needs special handling because bookings
     * may already exist.
     */
    int bookedTickets = event.getCapacity()
        - event.getAvailableTickets();

    if (request.capacity() < bookedTickets) {
      throw new InvalidEventException(
          "Capacity cannot be less than already booked tickets");
    }

    event.setCapacity(request.capacity());
    event.setAvailableTickets(
        request.capacity() - bookedTickets);

    EventEntity savedEvent = eventRepository.save(event);

    outboxService.createEventUpdateNotificationJob(
        new EventUpdateNotificationJob(
            savedEvent.getId(),
            savedEvent.getOrganizerId()));

    UserEntity organizer = authRepository.findById(
        savedEvent.getOrganizerId()).orElseThrow(() -> new RuntimeException("Organizer not found"));

    EventResponseObj responseObj = convertEventAndOrganizerToEventResponse(savedEvent, organizer);

    return new EventResponseDTO(
        "Event Updated Successfully",
        HttpStatus.OK.value(),
        responseObj);
  }

  public DeleteEventResponseDTO deleteEventById(Long eventId) {

    EventEntity event = eventRepository.findById(eventId)
        .orElseThrow(() -> new EventNotFoundException("Event not found"));

    requireAdminRole("Only admins can delete events");

    Long organizerId = getAuthenticatedUserId();

    // Only the organizer who created the event can delete it
    if (!event.getOrganizerId().equals(organizerId)) {
      throw new UnauthorizedException(
          "You are not authorized to delete this event");
    }

    eventRepository.delete(event);

    return new DeleteEventResponseDTO(
        "Event Deleted Successfully",
        HttpStatus.OK.value());
  }

}
