package com.booking.event.events.dto.event;

import java.time.Instant;

public record EventResponseObj(
        Long id,
        String organizerName,
        String name,
        String description,
        String venue,
        Instant startTime,
        Instant endTime,
        Integer capacity,
        Integer availableTickets,
        Instant createdAt
) {}
