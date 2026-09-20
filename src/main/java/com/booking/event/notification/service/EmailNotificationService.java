package com.booking.event.notification.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.booking.event.auth.entity.UserEntity;
import com.booking.event.booking.entity.BookingEntity;
import com.booking.event.booking.repository.BookingRepository;
import com.booking.event.events.entity.EventEntity;
import com.booking.event.events.repository.EventRepository;
import com.booking.event.notification.email.EmailClient;
import com.booking.event.notification.email.EmailMessage;
import com.booking.event.notification.job.BookingConfirmationJob;
import com.booking.event.notification.job.EventUpdateNotificationJob;

@Service
public class EmailNotificationService implements NotificationService {
    private static final Logger logger =
            LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
                    .withZone(ZoneId.systemDefault());

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final EmailClient emailClient;

    public EmailNotificationService(
            BookingRepository bookingRepository,
            EventRepository eventRepository,
            EmailClient emailClient) {
        this.bookingRepository = bookingRepository;
        this.eventRepository = eventRepository;
        this.emailClient = emailClient;
    }

    @Override
    public void processBookingConfirmation(BookingConfirmationJob job) {
        bookingRepository.findByIdWithEventAndCustomer(job.bookingId())
                .ifPresentOrElse(
                        this::sendBookingConfirmation,
                        () -> logger.warn(
                                "Skipping booking confirmation email. Booking {} was not found.",
                                job.bookingId()));
    }

    @Override
    public void processEventUpdateNotification(EventUpdateNotificationJob job) {
        EventEntity event = eventRepository.findById(job.eventId())
                .orElse(null);

        if (event == null) {
            logger.warn(
                    "Skipping event update emails. Event {} was not found.",
                    job.eventId());
            return;
        }

        List<UserEntity> customers =
                bookingRepository.findDistinctCustomersByEventId(event.getId());

        for (UserEntity customer : customers) {
            sendEventUpdate(event, customer);
        }
    }

    private void sendBookingConfirmation(BookingEntity booking) {
        EventEntity event = booking.getEvent();
        UserEntity customer = booking.getCustomer();

        String subject = "Booking confirmed: " + event.getName();
        String text = """
                Hi %s,

                Your booking is confirmed.

                Booking ID: %d
                Event: %s
                Venue: %s
                Starts: %s
                Ends: %s
                Quantity: %d
                """.formatted(
                customer.getName(),
                booking.getId(),
                event.getName(),
                event.getVenue(),
                formatInstant(event.getStartTime()),
                formatInstant(event.getEndTime()),
                booking.getQuantity());

        sendSafely(customer.getEmail(), subject, text);
    }

    private void sendEventUpdate(EventEntity event, UserEntity customer) {
        String subject = "Event updated: " + event.getName();
        String text = """
                Hi %s,

                An event you booked has been updated.

                Event: %s
                Venue: %s
                Starts: %s
                Ends: %s
                Available tickets: %d
                """.formatted(
                customer.getName(),
                event.getName(),
                event.getVenue(),
                formatInstant(event.getStartTime()),
                formatInstant(event.getEndTime()),
                event.getAvailableTickets());

        sendSafely(customer.getEmail(), subject, text);
    }

    private void sendSafely(String to, String subject, String text) {
        try {
            emailClient.send(new EmailMessage(to, subject, text));
        } catch (Exception exception) {
            logger.error("Failed to send notification email to {}", to, exception);
        }
    }

    private String formatInstant(java.time.Instant instant) {
        return DATE_TIME_FORMATTER.format(instant);
    }
}
