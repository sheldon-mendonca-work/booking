package com.booking.event.events.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.booking.event.auth.constants.UserRoleEnum;
import com.booking.event.auth.entity.UserEntity;
import com.booking.event.auth.repository.AuthRepository;
import com.booking.event.events.dto.event.CreateEventRequestDTO;
import com.booking.event.events.dto.event.EventResponseDTO;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.InvalidEventException;
import com.booking.event.global.exception.events.UnauthorizedException;
import com.booking.event.outbox.OutboxService;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    private EventRepository eventRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private OutboxService outboxService;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository,
                authRepository,
                outboxService);
        authenticate(20L, "ROLE_ADMIN");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createEventRequiresAdminRole() {
        authenticate(10L, "ROLE_USER");

        assertThrows(UnauthorizedException.class,
                () -> eventService.createEvent(validRequest(50)));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEventCreatesEventForAuthenticatedAdmin() {
        when(eventRepository.save(any(EventEntity.class)))
                .thenAnswer(invocation -> {
                    EventEntity event = invocation.getArgument(0);
                    event.setId(1L);
                    return event;
                });
        when(authRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, UserRoleEnum.ADMIN)));

        EventResponseDTO response = eventService.createEvent(validRequest(50));

        assertEquals(201, response.statusCode());
        ArgumentCaptor<EventEntity> eventCaptor =
                ArgumentCaptor.forClass(EventEntity.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals(20L, eventCaptor.getValue().getOrganizerId());
        assertEquals(50, eventCaptor.getValue().getCapacity());
        assertEquals(50, eventCaptor.getValue().getAvailableTickets());
    }

    @Test
    void updateEventRequiresAdminRole() {
        authenticate(10L, "ROLE_USER");

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event(1L, 10L, 50, 40)));

        assertThrows(UnauthorizedException.class,
                () -> eventService.editEventById(1L, validRequest(50)));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventRequiresOwnership() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event(1L, 99L, 50, 40)));

        assertThrows(UnauthorizedException.class,
                () -> eventService.editEventById(1L, validRequest(50)));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void updateEventPersistsAndCreatesOutboxForOwnerAdmin() {
        EventEntity event = event(1L, 20L, 50, 40);
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(event))
                .thenReturn(event);
        when(authRepository.findById(20L))
                .thenReturn(Optional.of(user(20L, UserRoleEnum.ADMIN)));

        EventResponseDTO response = eventService.editEventById(1L, validRequest(60));

        assertEquals(200, response.statusCode());
        assertEquals(60, event.getCapacity());
        assertEquals(50, event.getAvailableTickets());
        verify(outboxService).createEventUpdateNotificationJob(any());
    }

    @Test
    void updateEventThrowsWhenEventMissing() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> eventService.editEventById(1L, validRequest(50)));
    }

    @Test
    void updateEventRejectsEndBeforeStart() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event(1L, 20L, 50, 40)));

        CreateEventRequestDTO request = new CreateEventRequestDTO(
                "Spring Fest",
                "Updated",
                "Main Hall",
                Instant.now().plusSeconds(7200),
                Instant.now().plusSeconds(3600),
                50);

        assertThrows(InvalidEventException.class,
                () -> eventService.editEventById(1L, request));
    }

    @Test
    void deleteEventRequiresOwnership() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event(1L, 99L, 50, 40)));

        assertThrows(UnauthorizedException.class,
                () -> eventService.deleteEventById(1L));

        verify(eventRepository, never()).delete(any());
    }

    private void authenticate(Long userId, String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        java.util.List.of(new SimpleGrantedAuthority(role)));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CreateEventRequestDTO validRequest(Integer capacity) {
        return new CreateEventRequestDTO(
                "Spring Fest",
                "Updated",
                "Main Hall",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                capacity);
    }

    private EventEntity event(
            Long id,
            Long organizerId,
            Integer capacity,
            Integer availableTickets) {
        EventEntity event = new EventEntity();
        event.setId(id);
        event.setOrganizerId(organizerId);
        event.setName("Spring Fest");
        event.setDescription("Original");
        event.setVenue("Main Hall");
        event.setStartTime(Instant.now().plusSeconds(3600));
        event.setEndTime(Instant.now().plusSeconds(7200));
        event.setCapacity(capacity);
        event.setAvailableTickets(availableTickets);
        return event;
    }

    private UserEntity user(Long id, UserRoleEnum role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
    }
}
