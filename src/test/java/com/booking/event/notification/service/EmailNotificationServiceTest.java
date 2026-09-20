package com.booking.event.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.booking.event.auth.constants.UserRoleEnum;
import com.booking.event.auth.entity.UserEntity;
import com.booking.event.booking.constants.BookingStatusEnum;
import com.booking.event.booking.entity.BookingEntity;
import com.booking.event.booking.repository.BookingRepository;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.notification.email.EmailClient;
import com.booking.event.notification.email.EmailMessage;
import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {
    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EmailClient emailClient;

    private EmailNotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new EmailNotificationService(
                bookingRepository,
                eventRepository,
                emailClient);
    }

    @Test
    void processBookingConfirmationSendsEmailToBookingCustomer() {
        EventEntity event = event(1L);
        UserEntity customer = user(10L, "customer@example.com");
        BookingEntity booking = booking(100L, event, customer, 2);

        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.of(booking));

        notificationService.processBookingConfirmation(
                new BookingConfirmationJob(100L, 1L, 10L));

        ArgumentCaptor<EmailMessage> messageCaptor =
                ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailClient).send(messageCaptor.capture());

        EmailMessage message = messageCaptor.getValue();
        assertEquals("customer@example.com", message.to());
        assertTrue(message.subject().contains("Spring Fest"));
        assertTrue(message.text().contains("Booking ID: 100"));
        assertTrue(message.text().contains("Quantity: 2"));
        assertTrue(message.text().contains("Main Hall"));
    }

    @Test
    void processBookingConfirmationSkipsMissingBooking() {
        when(bookingRepository.findByIdWithEventAndCustomer(100L))
                .thenReturn(Optional.empty());

        notificationService.processBookingConfirmation(
                new BookingConfirmationJob(100L, 1L, 10L));

        verify(emailClient, never()).send(any());
    }

    @Test
    void processEventUpdateNotificationSendsEmailToBookedCustomers() {
        EventEntity event = event(1L);
        UserEntity firstCustomer = user(10L, "first@example.com");
        UserEntity secondCustomer = user(11L, "second@example.com");

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.findDistinctCustomersByEventId(1L))
                .thenReturn(List.of(firstCustomer, secondCustomer));

        notificationService.processEventUpdateNotification(
                new EventUpdateNotificationJob(1L, 20L));

        ArgumentCaptor<EmailMessage> messageCaptor =
                ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailClient, org.mockito.Mockito.times(2))
                .send(messageCaptor.capture());

        List<EmailMessage> messages = messageCaptor.getAllValues();
        assertEquals("first@example.com", messages.get(0).to());
        assertEquals("second@example.com", messages.get(1).to());
        assertTrue(messages.get(0).text().contains("An event you booked has been updated"));
        assertTrue(messages.get(1).text().contains("Spring Fest"));
    }

    @Test
    void processEventUpdateNotificationSkipsMissingEvent() {
        when(eventRepository.findById(1L))
                .thenReturn(Optional.empty());

        notificationService.processEventUpdateNotification(
                new EventUpdateNotificationJob(1L, 20L));

        verify(bookingRepository, never()).findDistinctCustomersByEventId(any());
        verify(emailClient, never()).send(any());
    }

    @Test
    void processEventUpdateNotificationContinuesAfterRecipientFailure() {
        EventEntity event = event(1L);
        UserEntity firstCustomer = user(10L, "first@example.com");
        UserEntity secondCustomer = user(11L, "second@example.com");

        when(eventRepository.findById(1L))
                .thenReturn(Optional.of(event));
        when(bookingRepository.findDistinctCustomersByEventId(1L))
                .thenReturn(List.of(firstCustomer, secondCustomer));
        doThrow(new RuntimeException("provider down"))
                .doNothing()
                .when(emailClient)
                .send(any());

        notificationService.processEventUpdateNotification(
                new EventUpdateNotificationJob(1L, 20L));

        verify(emailClient, org.mockito.Mockito.times(2)).send(any());
    }

    private EventEntity event(Long id) {
        EventEntity event = new EventEntity();
        event.setId(id);
        event.setOrganizerId(20L);
        event.setName("Spring Fest");
        event.setDescription("Updated event");
        event.setVenue("Main Hall");
        event.setStartTime(Instant.parse("2026-10-01T10:00:00Z"));
        event.setEndTime(Instant.parse("2026-10-01T12:00:00Z"));
        event.setCapacity(100);
        event.setAvailableTickets(50);
        return event;
    }

    private UserEntity user(Long id, String email) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail(email);
        user.setPasswordHash("hash");
        user.setRole(UserRoleEnum.USER);
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
