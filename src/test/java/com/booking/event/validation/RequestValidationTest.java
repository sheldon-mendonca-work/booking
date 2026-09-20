package com.booking.event.validation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.booking.event.booking.dto.booking.BookingCreateRequestDTO;
import com.booking.event.events.dto.event.CreateEventRequestDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RequestValidationTest {
    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void bookingQuantityMustBeAtLeastOne() {
        assertTrue(validator.validate(new BookingCreateRequestDTO(0))
                .stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("Quantity must be at least 1")));
    }

    @Test
    void eventCapacityMustBeAtLeastOne() {
        CreateEventRequestDTO request = new CreateEventRequestDTO(
                "Spring Fest",
                "Description",
                "Main Hall",
                Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200),
                0);

        assertTrue(validator.validate(request)
                .stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("Capacity must be at least 1")));
    }

    @Test
    void eventDatesMustBeInFuture() {
        CreateEventRequestDTO request = new CreateEventRequestDTO(
                "Spring Fest",
                "Description",
                "Main Hall",
                Instant.now().minusSeconds(7200),
                Instant.now().minusSeconds(3600),
                10);

        assertTrue(validator.validate(request)
                .stream()
                .anyMatch(violation -> violation.getMessage()
                        .equals("Start time must be in the future")));
    }
}
