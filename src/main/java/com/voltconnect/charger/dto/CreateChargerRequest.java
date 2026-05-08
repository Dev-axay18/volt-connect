package com.voltconnect.charger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for creating a new charger listing.
 *
 * <p>All required fields are validated via Bean Validation annotations.
 * Satisfies Requirements 4.1, 4.2, 4.3.
 */
public record CreateChargerRequest(

        @NotBlank(message = "Title must not be blank")
        String title,

        String description,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        @NotBlank(message = "Address must not be blank")
        String address,

        @NotEmpty(message = "At least one connector type is required")
        List<String> connectorType,

        @NotNull(message = "Power (kW) is required")
        @Positive(message = "Power (kW) must be positive")
        BigDecimal powerKw,

        @NotNull(message = "Price per hour is required")
        @Positive(message = "Price per hour must be positive")
        BigDecimal pricePerHour,

        List<String> photos
) {}
