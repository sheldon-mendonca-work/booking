package com.booking.event.global.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.booking.event.global.dto.ErrorResponseDTO;
import com.booking.event.global.exception.auth.InvalidCredentialsError;
import com.booking.event.global.exception.auth.UserDoesNotExistError;
import com.booking.event.global.exception.booking.BookingNotFoundException;
import com.booking.event.global.exception.booking.InvalidBookingException;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.UnauthorizedException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingResourcesToNotFound() {
        assertStatus(HttpStatus.NOT_FOUND,
                handler.handleEventNotFound(new EventNotFoundException("Event not found")));
        assertStatus(HttpStatus.NOT_FOUND,
                handler.handleBookingNotFoundException(new BookingNotFoundException("Booking not found")));
        assertStatus(HttpStatus.NOT_FOUND,
                handler.handleUserDoesNotExist(new UserDoesNotExistError("User not found")));
    }

    @Test
    void mapsForbiddenAndAuthenticationSeparately() {
        assertStatus(HttpStatus.FORBIDDEN,
                handler.handleUnauthorizedException(new UnauthorizedException("Forbidden")));
        assertStatus(HttpStatus.UNAUTHORIZED,
                handler.handleInvalidCredentials(new InvalidCredentialsError("Invalid credentials")));
    }

    @Test
    void mapsBusinessValidationToBadRequest() {
        assertStatus(HttpStatus.BAD_REQUEST,
                handler.handleInvalidBookingException(
                        new InvalidBookingException("Not enough tickets available")));
    }

    private void assertStatus(
            HttpStatus expected,
            ResponseEntity<ErrorResponseDTO> response) {
        assertEquals(expected, response.getStatusCode());
        assertEquals(expected.value(), response.getBody().status());
    }
}
