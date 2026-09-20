package com.booking.event.booking.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingCreateRequestDTO(

        @NotNull (message = "Quantity is required")
        @Min (value = 1, message = "Quantity must be at least 1")
        Integer quantity

) {
}
