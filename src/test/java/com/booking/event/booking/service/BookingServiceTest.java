package com.booking.event.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.booking.event.auth.constants.UserRoleEnum;
import com.booking.event.auth.entity.UserEntity;
import com.booking.event.auth.repository.AuthRepository;
import com.booking.event.booking.constants.BookingStatusEnum;
import com.booking.event.booking.dto.booking.BookingCreateRequestDTO;
import com.booking.event.booking.dto.booking.BookingResponseDTO;
import com.booking.event.booking.entity.BookingEntity;
import com.booking.event.booking.repository.BookingRepository;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.global.exception.auth.UserDoesNotExistError;
import com.booking.event.global.exception.booking.BookingNotFoundException;
import com.booking.event.global.exception.booking.InvalidBookingException;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.UnauthorizedException;
import com.booking.event.outbox.OutboxService;

import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private OutboxService outboxService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository,
                eventRepository,
                authRepository,
                outboxService);
        authenticate(10L, "ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBookingDecrementsAvailableTicketsAndCreatesSuccessfulBooking() {
        EventEntity event = event(1L, 20L, 10, 10);
        UserEntity customer = user(10L, UserRoleEnum.USER);

        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));
        when(authRepository.findById(10L))
                .thenReturn(Optional.of(customer));
        when(bookingRepository.save(any(BookingEntity.class)))
                .thenAnswer(invocation -> {
                    BookingEntity booking = invocation.getArgument(0);
                    booking.setId(100L);
                    return booking;
                });

        BookingResponseDTO response = bookingService.createBookingByEventId(
                1L,
                new BookingCreateRequestDTO(3));

        assertEquals(201, response.statusCode());
        assertEquals(BookingStatusEnum.SUCCESSFUL, response.data().status());
        assertEquals(7, event.getAvailableTickets());

        ArgumentCaptor<BookingEntity> bookingCaptor =
                ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertEquals(event, bookingCaptor.getValue().getEvent());
        assertEquals(customer, bookingCaptor.getValue().getCustomer());
        assertEquals(3, bookingCaptor.getValue().getQuantity());
        verify(outboxService).createBookingConfirmationJob(any());
    }

    @Test
    void createBookingRequiresCustomerRole() {
        authenticate(1L, "ROLE_ADMIN");

        assertThrows(UnauthorizedException.class,
                () -> bookingService.createBookingByEventId(
                        1L,
                        new BookingCreateRequestDTO(1)));

        verify(eventRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingThrowsWhenEventDoesNotExist() {
        when(eventRepository.findByIdForUpdate(99L))
                .thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> bookingService.createBookingByEventId(
                        99L,
                        new BookingCreateRequestDTO(1)));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingThrowsWhenTicketsAreInsufficient() {
        EventEntity event = event(1L, 20L, 10, 2);
        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        assertThrows(InvalidBookingException.class,
                () -> bookingService.createBookingByEventId(
                        1L,
                        new BookingCreateRequestDTO(3)));

        assertEquals(2, event.getAvailableTickets());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingThrowsForInvalidQuantity() {
        assertThrows(InvalidBookingException.class,
                () -> bookingService.createBookingByEventId(
                        1L,
                        new BookingCreateRequestDTO(0)));

        verify(eventRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBookingThrowsWhenCustomerDoesNotExist() {
        EventEntity event = event(1L, 20L, 10, 10);
        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));
        when(authRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(UserDoesNotExistError.class,
                () -> bookingService.createBookingByEventId(
                        1L,
                        new BookingCreateRequestDTO(1)));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void deleteBookingRestoresTicketsAndDeletesBooking() {
        EventEntity event = event(1L, 20L, 10, 4);
        BookingEntity booking = booking(100L, event, user(10L, UserRoleEnum.USER), 3);

        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.of(booking));
        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        bookingService.deleteBookingById(100L);

        assertEquals(7, event.getAvailableTickets());
        verify(bookingRepository).delete(booking);
    }

    @Test
    void deleteBookingThrowsWhenBookingDoesNotExist() {
        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.deleteBookingById(100L));

        verify(eventRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void deleteBookingThrowsWhenUserDoesNotOwnBooking() {
        EventEntity event = event(1L, 20L, 10, 4);
        BookingEntity booking = booking(100L, event, user(11L, UserRoleEnum.USER), 3);

        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedException.class,
                () -> bookingService.deleteBookingById(100L));

        verify(eventRepository, never()).findByIdForUpdate(any());
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void deleteBookingRequiresCustomerRole() {
        authenticate(1L, "ROLE_ADMIN");

        assertThrows(UnauthorizedException.class,
                () -> bookingService.deleteBookingById(100L));

        verify(bookingRepository, never()).findByIdWithEventAndCustomer(any());
    }

    @Test
    void deleteBookingThrowsWhenTicketRestoreWouldExceedCapacity() {
        EventEntity event = event(1L, 20L, 10, 9);
        BookingEntity booking = booking(100L, event, user(10L, UserRoleEnum.USER), 3);

        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.of(booking));
        when(eventRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(event));

        assertThrows(InvalidBookingException.class,
                () -> bookingService.deleteBookingById(100L));

        assertEquals(9, event.getAvailableTickets());
        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void getBookingByIdAllowsOrganizerToReadEventBooking() {
        authenticate(20L, "ROLE_USER");
        EventEntity event = event(1L, 20L, 10, 4);
        BookingEntity booking = booking(100L, event, user(10L, UserRoleEnum.USER), 3);

        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.of(booking));

        BookingResponseDTO response = bookingService.getBookingById(100L);

        assertEquals(100L, response.data().id());
        assertEquals(10L, response.data().customerId());
    }

    @Test
    void getBookingsByEventIdThrowsWhenEventDoesNotExist() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,
                () -> bookingService.getBookingsByEventId(1L));
    }

    @Test
    void getAllBookingsRequiresAdmin() {
        assertThrows(UnauthorizedException.class,
                () -> bookingService.getAllBookings());
    }

    @Test
    void getAllBookingsAllowsAdmin() {
        authenticate(1L, "ROLE_ADMIN");
        EventEntity event = event(1L, 20L, 10, 4);
        BookingEntity booking = booking(100L, event, user(10L, UserRoleEnum.USER), 3);

        when(bookingRepository.findAllWithEventAndCustomer())
                .thenReturn(List.of(booking));

        assertEquals(1, bookingService.getAllBookings().data().size());
    }

    @Test
    void eventRepositoryUsesPessimisticWriteLockForBookingInventoryChanges()
            throws NoSuchMethodException {
        Method method = EventRepository.class
                .getMethod("findByIdForUpdate", Long.class);

        Lock lock = method.getAnnotation(Lock.class);

        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    private void authenticate(Long userId, String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority(role)));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private EventEntity event(
            Long id,
            Long organizerId,
            Integer capacity,
            Integer availableTickets) {
        EventEntity event = new EventEntity();
        event.setId(id);
        event.setOrganizerId(organizerId);
        event.setName("Concert");
        event.setDescription("Live event");
        event.setVenue("Hall");
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

    private BookingEntity booking(
            Long id,
            EventEntity event,
            UserEntity customer,
            Integer quantity) {
        BookingEntity booking = new BookingEntity();
        booking.setId(id);
        booking.setEvent(event);
        booking.setCustomer(customer);
        booking.setQuantity(quantity);
        booking.setStatus(BookingStatusEnum.SUCCESSFUL);
        return booking;
    }
}
