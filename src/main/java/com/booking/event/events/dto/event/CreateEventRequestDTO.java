package com.booking.event.events.dto.event;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequestDTO(

    @NotBlank(message = "Event name is required") @Size(max = 200, message = "Event name must not exceed 200 characters") String name,

    @Size(max = 2000, message = "Description must not exceed 2000 characters") String description,

    @NotBlank(message = "Venue is required") @Size(max = 500, message = "Venue must not exceed 500 characters") String venue,

    @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") Instant startTime,

    @NotNull(message = "End time is required") @Future(message = "End time must be in the future") Instant endTime,

    @NotNull(message = "Capacity is required") @Min(value = 1, message = "Capacity must be at least 1") Integer capacity) {
}