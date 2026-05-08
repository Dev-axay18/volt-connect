package com.voltconnect.charger.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Filter criteria applied in-memory after the geospatial nearby query.
 *
 * <p>All fields are optional. Null values mean "no filter applied" for that
 * dimension.
 *
 * <p>Satisfies Requirements 3.2, 13.1.
 */
public record ChargerFilterDto(

        /** Filter by connector type overlap (charger must support at least one). */
        List<String> connectorTypes,

        /** Minimum power output in kW (inclusive). */
        Double minPowerKw,

        /** Maximum price per hour in INR (inclusive). */
        BigDecimal maxPricePerHour,

        /** When {@code true}, only return chargers where {@code isAvailable = true}. */
        Boolean availableOnly
) {}
