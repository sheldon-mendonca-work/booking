package com.booking.event.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.booking.event.global.dto.ErrorResponseDTO;
import com.booking.event.global.exception.auth.InvalidCredentialsError;
import com.booking.event.global.exception.auth.UserAlreadyExistsError;
import com.booking.event.global.exception.auth.UserDoesNotExistError;
import com.booking.event.global.exception.booking.BookingNotFoundException;
import com.booking.event.global.exception.booking.InvalidBookingException;
import com.booking.event.global.exception.events.EventNotFoundException;
import com.booking.event.global.exception.events.InvalidEventException;
import com.booking.event.global.exception.events.UnauthorizedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(UserAlreadyExistsError.class)
  public ResponseEntity<ErrorResponseDTO> handleUserAlreadyExists(UserAlreadyExistsError exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(responseDTO);
  }

  @ExceptionHandler(UserDoesNotExistError.class)
  public ResponseEntity<ErrorResponseDTO> handleUserDoesNotExist(UserDoesNotExistError exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(responseDTO);
  }

  @ExceptionHandler(InvalidCredentialsError.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(InvalidCredentialsError exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.UNAUTHORIZED.value());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).body(responseDTO);
  }

  @ExceptionHandler(InvalidEventException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidEvent(InvalidEventException exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(responseDTO);
  }

  @ExceptionHandler(EventNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleEventNotFound(EventNotFoundException exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(responseDTO);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(UnauthorizedException exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.FORBIDDEN.value());
    return ResponseEntity.status(HttpStatus.FORBIDDEN.value()).body(responseDTO);
  }

  @ExceptionHandler (BookingNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleBookingNotFoundException(BookingNotFoundException exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.NOT_FOUND.value());
    return ResponseEntity.status(HttpStatus.NOT_FOUND.value()).body(responseDTO);
  }
  
  @ExceptionHandler (InvalidBookingException.class)
  public ResponseEntity<ErrorResponseDTO> handleInvalidBookingException(InvalidBookingException exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(responseDTO);
  }

  @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
  public ResponseEntity<ErrorResponseDTO> handleAuthentication(Exception exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        "Authentication is required",
        HttpStatus.UNAUTHORIZED.value());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value()).body(responseDTO);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleValidation(
      MethodArgumentNotValidException exception) {

    String message = exception.getBindingResult()
        .getFieldErrors()
        .getFirst()
        .getDefaultMessage();

    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        message,
        HttpStatus.BAD_REQUEST.value());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(responseDTO);
  }



  @ExceptionHandler
  public ResponseEntity<ErrorResponseDTO> handleGlobalException(Exception exception) {
    ErrorResponseDTO responseDTO = new ErrorResponseDTO(
        exception.getMessage(),
        HttpStatus.INTERNAL_SERVER_ERROR.value());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(responseDTO);
  }
}
