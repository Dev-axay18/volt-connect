package com.voltconnect.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Request body for creating a new booking.
 *
 * <p>Satisfies Requirements 6.1, 6.2.
 */
public record CreateBookingRequest(

    @NotNull(message = "chargerId is required")
    UUID chargerId,

    @NotNull(message = "startTime is required")
    Instant startTime,

    @NotNull(message = "endTime is required")
    Instant endTime
) {}
